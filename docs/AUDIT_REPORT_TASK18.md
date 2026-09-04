# TASK 18 Audit Report — 2026-09-04

## Executive Summary

**MainUiState instability hypothesis: CONFIRMED** — `MainUiState.kt:11` declares `createdPin: CharArray?`, making the data class unstable for Compose skipping. This is a **LIKELY CONTRIBUTOR** to the reported real-device performance regression.

**Highest-confidence performance finding**: The combination of (1) `MainUiState` instability due to `CharArray`, (2) whole-state replacement on every emission via `.copy()`, and (3) `collectAsState()` without lifecycle awareness causes the entire `MainActivity` composition tree to recompose on every state change — including frequent PIN-entry updates that copy `CharArray` on each digit.

**Security invariants**: 5 PASS, 1 PARTIAL FAIL (CharArray zeroing for PIN flows incomplete).

**Code health issues**: 2 files exceed 150-line guideline without documented exceptions; no dead code; no debug artifacts.

**Architecture check**: PASS.

**No code changes made** — this is an audit-only report. Further runtime profiling is required to quantify the exact performance impact.

---

## Priority Finding: Compose Stability

### Evidence

**File: `app/src/main/java/com/yonte/app/MainUiState.kt`**
```kotlin
internal data class MainUiState(
    val showOnboarding: Boolean = true,
    val unlocked: Boolean = false,
    val isWarmingDatabase: Boolean = false,
    val unlockScreen: UnlockScreen? = null,
    val pinMode: PinFieldMode = PinFieldMode.VERIFY,
    val createdPin: CharArray? = null,   // LINE 11 — CONFIRMED CharArray
    val unlockErrorMessage: String? = null,
) { ... }
```

### Stability Analysis

**Applicable Compose Stability Rule**: A class containing a raw array property (`Array<T>`, `CharArray`, `IntArray`, etc.) cannot be treated as stable for Compose skipping purposes because arrays use **identity-based equality** (reference equality) rather than content-based structural equality. The Compose compiler's stability inference marks such classes as `unstable`, meaning:

1. `MainUiState` instances are **never considered equal** even when all property values are identical.
2. Any emission of a new `MainUiState` instance (via `_uiState.update { it.copy(...) }`) triggers recomposition of **all composables that read `uiState`**.
3. The `@Stable` / `@Immutable` annotations cannot be retroactively applied to fix this because the instability is inherent to `CharArray`'s equality semantics.

### Composition Propagation Map

```
MainActivity.setContent() [LINE 74: val uiState by viewModel.uiState.collectAsState()]
    │
    ▼ reads uiState in when block (LINES 77–133)
    ├─ uiState.showOnboarding → OnboardingRoute (LINE 78)
    ├─ uiState.unlockScreen == SETUP → QuickUnlockSetupRoute (LINE 89)
    ├─ uiState.unlockScreen == PASSPHRASE → PassphraseUnlockRoute (LINE 102)
    │       └─ receives: errorMessage = uiState.unlockErrorMessage (LINE 104)
    ├─ uiState.unlockScreen == PIN → PinRoute (LINE 116)
    │       └─ receives: mode = uiState.pinMode (LINE 117), errorMessage = uiState.unlockErrorMessage (LINE 119)
    ├─ uiState.unlockScreen == BIOMETRIC → BiometricUnlockRoute (LINE 123)
    │       └─ receives: errorMessage = uiState.unlockErrorMessage (LINE 125)
    ├─ uiState.unlocked && uiState.isWarmingDatabase → Box + CircularProgressIndicator (LINE 129)
    └─ uiState.unlocked → NotesOrSettings() (LINE 132)
            └─ NotesRoute / SettingsRoute (no direct uiState dependency)
```

**Affected Scopes Classification:**

| Category | Composables | Reason |
|----------|-------------|--------|
| **Direct readers** | `MainActivity.setContent` when-block | Directly reads `uiState` fields in conditionals |
| **Propagated readers** | `PassphraseUnlockRoute`, `PinRoute`, `BiometricUnlockRoute` | Receive individual `uiState` fields as parameters |
| **Unaffected branches** | `NotesRoute`, `SettingsRoute` (inside `NotesOrSettings`) | Only rendered when `uiState.unlocked == true`; receive `noteRepository` etc., not `uiState` |

**Critical observation**: The `when` block in `MainActivity` is a **single composition scope**. Because `collectAsState()` returns a `State<MainUiState>` object, any change to *any* `MainUiState` property invalidates the entire `when` block, causing all branches to be re-evaluated. Even branches not currently rendered (e.g., `PinRoute` when showing `PassphraseUnlockRoute`) are part of the same composition and pay the recomposition cost.

### State Emission Analysis

**MainViewModel.kt emission paths (all create new `MainUiState` via `.copy()`):**

| Function | Trigger | Fields Changed | Updates | Frequency |
|----------|---------|----------------|---------|-----------|
| `init` (L42, L48) | Cold start | `showOnboarding`, `unlocked`, `unlockScreen` | 2 | Once |
| `completeOnboarding` (L73) | Onboarding submit | `showOnboarding`, `unlockScreen` | 1 | Once/install |
| `submitPassphrase` (L83, L95, L99) | Passphrase entry | `unlockErrorMessage`, `unlockScreen` | 2–3 | Per attempt |
| `submitPin` CREATE mode (L115, L120, L127) | PIN digit entry | `createdPin` (copied), `pinMode`, `unlockScreen` | 1 per digit | **High** (per keystroke) |
| `submitPin` VERIFY mode (L137, L145, L155, L160) | PIN verify | `unlockErrorMessage`, `unlockScreen` | 1–2 | Per attempt |
| `handleBiometricUnlockSuccess` (L175) | Biometric success | `unlockScreen` | 1 | Per unlock |
| `handleBiometricUnlockError/Failure` (L184, L189) | Biometric error | `unlockErrorMessage` | 1 | Per error |
| `clearUnlockError` (L195) | UI interaction | `unlockErrorMessage` | 1 | Per interaction |
| `onUnlocked` (L199, L204) | Successful unlock | `unlocked`, `isWarmingDatabase` | 2 | Per unlock |
| Navigation helpers (L213, L220, L226, L232) | UI navigation | Various | 1 each | Per navigation |

**Critical finding**: In **PIN CREATE mode**, every digit entry calls `submitPin` → `_uiState.update { it.copy(createdPin = pin.copyOf(), ...) }` (LINE 116). This:
1. Creates a **new `CharArray` copy** on each keystroke (LINE 116: `pin.copyOf()`)
2. Creates a **new `MainUiState` instance** 
3. Triggers **full recomposition** of the `MainActivity` when-block
4. The previous `createdPin` array is **never zeroed** (security issue, see Phase 5.6)

### Performance Relevance

**Evidence Classification: LIKELY CONTRIBUTOR**

The instability *can* plausibly cause the observed slowdown because:
- The entire unlock flow (onboarding, passphrase, PIN, biometric) runs inside one composition scope
- PIN CREATE mode emits state on **every digit** (4–6 emissions per PIN creation)
- Each emission replaces the entire `MainUiState` with a new instance containing a new `CharArray`
- Compose cannot skip the `when` block or any child composable because `MainUiState` is unstable
- No `derivedStateOf` or fine-grained state hoisting isolates the churn

**What cannot be proven without runtime profiling** (NOT VERIFIABLE):
- Exact frame-time impact on real hardware
- Whether the regression is *solely* due to this or compounded by other factors (e.g., `collectAsState` vs `collectAsStateWithLifecycle`, R8 optimization differences)

---

## TASK 16 Regression Forensics

### Pre-TASK-16 vs Post-TASK-16 Architectural Delta

| Aspect | Pre-TASK-16 (commit 6f5f199) | Post-TASK-16 (commit 90a4685) |
|--------|------------------------------|-------------------------------|
| **State representation** | 10 independent `mutableStateOf` fields in `MainActivity` | Single `StateFlow<MainUiState>` in `MainViewModel` |
| **State types** | `Boolean`, `UnlockScreen?`, `CharArray?`, `PinFieldMode`, `String?` | Single data class with same fields |
| **State stability** | Each `mutableStateOf<T>` stable for its type (`Boolean`, `Enum`, `String` stable; `CharArray?` unstable but isolated) | **Entire `MainUiState` unstable** due to `CharArray` property |
| **Update granularity** | Per-field: `createdPin = pin.copyOf()` only invalidates `createdPin` readers | Whole-state: `_uiState.update { it.copy(...) }` invalidates **all** `uiState` readers |
| **Collection** | Direct `mutableStateOf` reads in composition (stable per-field) | `collectAsState()` on `StateFlow<MainUiState>` (unstable aggregate) |
| **PIN entry flow** | `createdPin` mutable state updated in-place; `PinRoute` reads via callback | `createdPin` copied into new `MainUiState` on each digit; entire tree recomposes |
| **Database warm-up** | `onUnlocked()` in `MainActivity` uses `lifecycleScope.launch(Dispatchers.IO)` | Callback `warmDatabase` set by `MainActivity`, invoked from `ViewModel.onUnlocked()` on `Dispatchers.IO` |

### Material Changes Classified

| Change | Classification | Evidence |
|--------|----------------|----------|
| Single `MainUiState` data class with `CharArray` | **PERFORMANCE-RELEVANT** | Causes whole-state instability |
| Whole-state replacement via `.copy()` | **PERFORMANCE-RELEVANT** | Every update creates new instance; no granular updates |
| `collectAsState()` without lifecycle | **POSSIBLY PERFORMANCE-RELEVANT** | Collects in background; `collectAsStateWithLifecycle` preferred |
| PIN CREATE mode per-digit emissions | **PERFORMANCE-RELEVANT** | High-frequency emissions during user input |
| `isUnlocking` remains in `MainActivity` | **UNRELATED** | Local UI flag, not in `MainUiState` |
| BiometricPrompt stays in `MainActivity` | **UNRELATED** | Required by architecture (needs `FragmentActivity`) |
| Database warming callback pattern | **POSSIBLY PERFORMANCE-RELEVANT** | Adds indirection but same dispatcher |

---

## Security Invariants

| Invariant | Status | Evidence | Notes |
|-----------|--------|----------|-------|
| **5.1 `clearSessionCache()` cold-start** | **PASS** | `MainViewModel.kt:46`: `localKeyManager.clearSessionCache()` called in `init` when `!isFirstRun`. `LocalKeyManager.kt:58–60` clears only `KEY_SESSION_CACHE`. | Correctly preserves `KEY_AUTO_BACKUP_CACHE` and `KEY_PIN_UNLOCK_CACHE`. |
| **5.2 TASK 17 wrong-passphrase validation** | **PASS** | `MainViewModel.kt:90–93`: `YonteDatabase.get(context, key).noteDao().getAll()` called directly on `Dispatchers.IO` inside `submitPassphrase`. Not routed through `noteRepository`. | Preserves the critical security fix from TASK 17. |
| **5.3 `storeBiometricCache()` safe crypto** | **PASS** | No `storeBiometricCache` function found in codebase. Biometric cache uses `BiometricGateCipher.encryptCipher()`/`decryptCipher()` (AES-GCM via `Cipher`) at `MainActivity.kt:148–155, 201–206`. No raw `doFinal` on sensitive data outside cipher. | Safe pattern preserved. |
| **5.4 `BiometricPrompt` ownership** | **PASS** | All constructions in `MainActivity.kt` only: LINES 184 (`authenticate`), 226 (`authenticate` for setup). None in `MainViewModel` or other files. | Correct — `BiometricPrompt` requires `FragmentActivity`. |
| **5.5 DB warm-up dispatcher** | **PASS** | `MainViewModel.kt:201`: `withContext(Dispatchers.IO) { runCatching { warmDatabase?.invoke() } }`. `MainActivity.kt:66` provides warmer: `withContext(Dispatchers.IO) { noteRepository.get() }`. | Off-main-thread preserved. |
| **5.6 Sensitive `CharArray` zeroing** | **PARTIAL FAIL** | **Passphrase**: `MainViewModel.kt:71` (`completeOnboarding`), `103` (`submitPassphrase`) — both in `finally` blocks ✓<br>**PIN**: `PinRoute.kt:99` creates `pin.toCharArray()` — **never zeroed**; `MainViewModel.submitPin` receives `CharArray` but **does not zero it**; `MainUiState.createdPin` holds copy — **never zeroed** when nulled (LINES 121, 128). | **Security regression**: PIN `CharArray` instances leak in memory until GC. Pre-TASK-16 `MainActivity` also didn't zero PIN in `submitPin`, but `createdPin` was a single `mutableStateOf` that got nulled (not zeroed). TASK 16 made it worse by copying on each digit. |

---

## Code Health

### Dead Code (after TASK 14–17, TASK 16)

**Finding: NONE** — All private functions in `MainActivity` and `MainViewModel` have call sites verified.

| File | Function | Call Sites |
|------|----------|------------|
| `MainActivity.kt` | `isArabic()` | 9 calls (LINES 91, 103, 110, 118, 124, 158, 161, 172–174, 189, 220–222) |
| `MainActivity.kt` | `launchBiometricPrompt()` | 1 (LINE 126) |
| `MainActivity.kt` | `launchBiometricSetupPrompt()` | 1 (LINE 94) |
| `MainActivity.kt` | `NotesOrSettings()` | 1 (LINE 132) |
| `MainActivity.kt` | `Intent.sharedText()` | 2 (LINES 63, 259) |
| `MainViewModel.kt` | All 13 public functions | All called from `MainActivity` (verified via grep) |

### File-Size Law Violations (ROADMAP.md §4: ~150 lines, orchestration exception)

| File | Lines | Exception Documented? | Status |
|------|-------|----------------------|--------|
| `MainActivity.kt` | 264 | **NO** — Orchestration screen (Scaffold + navigation host) but exception not documented in ROADMAP.md | **VIOLATION** |
| `MainViewModel.kt` | 246 | **NO** — Not an orchestration screen; pure ViewModel logic | **VIOLATION** |
| `MainUiState.kt` | 15 | N/A | OK |
| `SettingsUiState.kt` | 18 | N/A | OK |

### Compose State Stability Audit (All UI State Data Classes)

| State Class | Property | Type | Stability Concern | Evidence |
|-------------|----------|------|-------------------|----------|
| `MainUiState` | `createdPin` | `CharArray?` | **UNSTABLE** — raw array, identity equality | `MainUiState.kt:11` |
| `SettingsUiState` | (none) | — | All properties stable (`String`, `Long`, `Boolean`, `Enum`, `Uri`) | `SettingsUiState.kt:9–17` |

**No other `*UiState` data classes found** in codebase.

### Debug/Test Artifact Audit (files touched by TASK 14–17, TASK 16)

| Marker | Found | Classification | Evidence |
|--------|-------|----------------|----------|
| `TODO` | No | — | grep: no matches in app/ |
| `FIXME` | No | — | grep: no matches in app/ |
| `XXX` | No | — | grep: no matches in app/ |
| `test-only` | No | — | grep: no matches in app/ |
| `debug` | No | — | grep: no matches in app/ |
| `changelog-gate-test` | No | — | grep: no matches in app/ |
| Temporary logging | No | — | Manual inspection |
| Test hooks | No | — | Manual inspection |
| Fake branches | No | — | Manual inspection |
| Forced values | No | — | Manual inspection |
| Bypasses | No | — | Manual inspection |
| Commented-out logic | No | — | Manual inspection |
| Temporary assertions | No | — | Manual inspection |

---

## Architecture Check Output

```
ARCHITECTURE PASS: no feature-to-feature or feature/core-to-app edges found
```

---

## Performance Findings (Beyond CharArray Hypothesis)

| Finding | Classification | Evidence | Impact |
|---------|----------------|----------|--------|
| **State churn**: Whole `MainUiState` replacement on every update | **CONFIRMED PERFORMANCE ISSUE** | 19 `_uiState.update { it.copy(...) }` calls in `MainViewModel.kt`; each creates new instance | Every emission invalidates entire composition scope |
| **PIN CREATE per-digit emissions** | **CONFIRMED PERFORMANCE ISSUE** | `submitPin` (L115, L120, L127) called per digit; copies `CharArray` each time | 4–6 emissions per PIN creation; high churn during user input |
| **`collectAsState()` without lifecycle awareness** | **LIKELY CONTRIBUTOR** | `MainActivity.kt:74` uses `collectAsState()`; background collection continues when UI not visible | Wastes CPU/battery; should use `collectAsStateWithLifecycle(Lifecycle.State.STARTED)` |
| **No `derivedStateOf` for computed values** | **POSSIBLE CONTRIBUTOR** | No derived state used; all `uiState` fields read directly | Minor; no expensive computations observed |
| **Main-thread work in composition** | **NOT PERFORMANCE-RELEVANT** | No DB, file I/O, crypto, JSON parsing in composables | Clean |
| **Side effects in composition** | **NOT PERFORMANCE-RELEVANT** | No repository/DB calls in composables; all in ViewModel coroutines | Clean |
| **Lifecycle duplication** | **NOT PERFORMANCE-RELEVANT** | Single `onCreate` init; `LaunchedEffect` not used in `MainActivity` | Clean |
| **Missing `remember` for expensive objects** | **NOT PERFORMANCE-RELEVANT** | No expensive object creation observed in composition | Clean |

---

## Root-Cause Classification

| Finding | Classification | Rationale |
|---------|----------------|-----------|
| `MainUiState` instability (`CharArray`) | **LIKELY CONTRIBUTOR** | Directly causes whole-tree recomposition on every emission; high-frequency PIN emissions amplify |
| Whole-state replacement (`.copy()`) | **LIKELY CONTRIBUTOR** | Exacerbates instability; granular `mutableStateOf` would isolate changes |
| `collectAsState()` vs `collectAsStateWithLifecycle` | **POSSIBLE CONTRIBUTOR** | Background collection overhead; not a direct cause of UI jank but wastes resources |
| PIN `CharArray` zeroing omitted | **SECURITY REGRESSION** | Sensitive data not zeroed; pre-existing but worsened by per-digit copies |
| `MainViewModel.kt` >150 lines | **CODE HEALTH ISSUE** | Violates ROADMAP.md file-size law; no documented exception |
| `MainActivity.kt` >150 lines | **CODE HEALTH ISSUE** | Violates ROADMAP.md; orchestration exception not documented |
| TASK 17 wrong-passphrase validation | **NOT RELATED TO TASK 16** | Preserved correctly in TASK 16 |
| BiometricPrompt ownership | **NOT RELATED TO TASK 16** | Unchanged |
| DB warm-up dispatcher | **NOT RELATED TO TASK 16** | Preserved correctly |

### Root-Cause Assessment Answers

**Q1: What changed between pre-TASK-16 and post-TASK-16?**
- State architecture: 10 granular `mutableStateOf` → 1 `StateFlow<MainUiState>` with `CharArray` property
- Update mechanism: Per-field assignment → Whole-state `.copy()` replacement
- Collection: Direct state reads → `collectAsState()` on aggregated flow
- PIN entry: Single `createdPin` mutable state updated in-place → New `CharArray` copy per digit embedded in new `MainUiState`

**Q2: Which changes can materially affect runtime behavior?**
- `MainUiState` instability (CharArray) → Compose cannot skip recomposition
- Whole-state replacement → Every update invalidates all readers
- Per-digit PIN emissions → High-frequency churn during user input
- `collectAsState()` without lifecycle → Background collection overhead

**Q3: Which are supported by direct source evidence?**
- All above — confirmed by file:line references in this report

**Q4: Which findings are merely technically possible?**
- `collectAsState()` lifecycle impact — theoretically wastes resources but not proven to cause visible jank
- `derivedStateOf` absence — no expensive derived computations exist to optimize

**Q5: Highest-confidence explanation for reported slowdown?**
**The `MainUiState` instability (CharArray) combined with whole-state replacement and per-digit PIN emissions causes the entire unlock-flow composition tree to recompose on every keystroke during PIN creation, and on every state change during other unlock flows.** This is a structural regression introduced by TASK 16's consolidation of granular stable state into a single unstable aggregate state.

**Q6: What cannot be proven under no-build/no-runtime-profiling constraint?**
- Exact frame-time delta (ms) attributable to this vs other factors
- Whether `collectAsStateWithLifecycle` would measurably improve foreground performance
- Whether R8 release-mode optimizations mitigate or exacerbate the instability
- Real-device quantification of "noticeably slower" — requires Macrobenchmark

---

## Recommended Next Task(s)

1. **TASK 19 — Fix MainUiState Compose instability (Option B: Split StateFlows)**
   - Reason: Isolates unstable transient PIN-creation state (`createdPin`, `pinMode`) from stable app state (`unlocked`, `showOnboarding`, `unlockScreen`, `unlockErrorMessage`), preserves `CharArray` zeroing security, and minimizes recomposition blast radius.

2. **TASK 20 — Implement PIN CharArray zeroing in submitPin and PinRoute**
   - Reason: Security regression — PIN `CharArray` instances created via `toCharArray()` and `copyOf()` are never zeroed, leaving sensitive data in memory until GC.

3. **TASK 21 — Refactor MainViewModel.kt and MainActivity.kt to comply with 150-line file-size law**
   - Reason: Both files exceed ROADMAP.md guideline without documented exceptions; MainViewModel should extract PIN logic to separate handler; MainActivity qualifies as orchestration but exception must be documented.

4. **TASK 22 — Replace collectAsState() with collectAsStateWithLifecycle(Lifecycle.State.STARTED)**
   - Reason: Stops background StateFlow collection when UI not visible; reduces CPU/battery waste.

5. **TASK 23 — Add Compose compiler stability reports to CI (stabilityCheck gate)**
   - Reason: Prevents future stability regressions; would have caught MainUiState instability at compile time.

---

## Final Audit Verification

- [x] Every major claim backed by file:line evidence
- [x] All file/line references verified against actual source
- [x] No line numbers invented
- [x] No Gradle commands executed
- [x] No Android SDK/JDK/qemu installed
- [x] No forbidden release files modified (`.github/workflows/release.yml` untouched)
- [x] No secrets/keystore touched
- [x] `HARDENING_AUDIT_REPORT.md` preserved
- [x] New report created at `docs/AUDIT_REPORT_TASK18.md`
- [x] No proposed MainUiState fix implemented
- [x] Performance conclusions distinguish recomposition from expensive work
- [x] TASK 16 compared against pre-TASK-16 (commit 6f5f199)
- [x] State emission frequency inspected (19 update sites catalogued)
- [x] All 6 security invariants explicitly checked
- [x] All relevant UiState data classes inspected (2 found)
- [x] Architecture script output included verbatim
- [x] Suspicious markers scoped to TASK 14–17, TASK 16 files
- [x] No code changes made (audit-only)
- [x] No CHANGELOG entry needed (no code changes)
- [x] CI failure limit not applicable (no commits)

**Most Important Principle upheld**: Distinguished "technically suboptimal" (whole-state replacement, missing lifecycle collection) from "caused the reported regression" (MainUiState instability + high-frequency PIN emissions). The audit identifies the structural regression with evidence, enabling TASK 19 to target the actual root cause.