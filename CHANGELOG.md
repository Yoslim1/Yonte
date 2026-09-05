# Changelog

## Unreleased — Fix Activity Context leak and SQLite cursor leak (2026-09-04)

- `MainViewModel.submitPassphrase` no longer takes an external `Context` parameter;
  it uses the already-injected `@ApplicationContext appContext` when validating the
  derived key via `YonteDatabase.get(appContext, key)`, so the Activity Context
  passed from `MainActivity` can no longer leak into the ViewModel scope.
  (`app/.../MainViewModel.kt`, `app/.../MainActivity.kt`)
- `NoteRepository.search()` now wraps the raw SQLite cursor from
  `database.openHelper.readableDatabase.query(...)` in `.use { }`, guaranteeing
  the cursor is closed even when an exception is thrown; the existing
  try/catch-and-fallback-to-`searchFallback` behavior is unchanged.
  (`core/database/.../NoteRepository.kt`)

## Unreleased — Fix Compose recomposition regression by isolating unstable PIN field (2026-09-04)

- Removed `createdPin: CharArray?` from `MainUiState` data class, which was making the
  entire state class unstable for Compose's recomposition-skipping. Moved the field to a
  plain private `var` in `MainViewModel` since it was never read by any composable.
  (`MainUiState.kt`, `MainViewModel.kt`)
- Added PIN `CharArray` zeroing in `submitPin` to match the established passphrase
  security pattern: zeroing the input `pin` in a `finally` block, and zeroing the held
  `createdPin` copy before releasing it on mismatch or success. (`MainViewModel.kt`)

## Unreleased — TASK 16: extract MainViewModel from MainActivity (2026-09-03)

- Split `MainActivity.kt` (418 lines) into a thin Activity + `MainViewModel`.
  `MainViewModel.kt` is `@HiltViewModel` — safe because its three injected
  dependencies (`LocalKeyManager`, `AppPinManager`, `BiometricGateCipher`) are
  SharedPreferences/AndroidKeyStore-only and never trigger `YonteDatabase`
  access. `noteRepository`, `backupGateway`, and `updateGateway` remain
  `@Inject` fields on `MainActivity`. `MainUiState.kt` holds the unlock state
  data class. `BiometricPrompt` construction stays in `MainActivity` (requires
  `FragmentActivity`). Added `MainViewModelTest` covering PIN verify success,
  lockout, passphrase fallback, and error clearing.

## Unreleased — fix wrong-passphrase crash (2026-09-03)

- `submitPassphrase` in `MainActivity.kt` now validates the derived key against the
  encrypted database (via `YonteDatabase.get().noteDao().getAll()`) inside the same
  `try` block that shows the "wrong passphrase" error. Previously Argon2 silently
  produced a valid but incorrect key for a wrong passphrase, `onUnlocked()` opened the
  database in the background with that wrong key, and the app crashed when Compose first
  tried to query notes. The fix calls `YonteDatabase.get()` directly (not through
  Hilt's `noteRepository`) so a wrong key does not poison the Hilt-managed singletons;
  `YonteDatabase`'s companion singleton handles key-change cleanup via its
  `instanceKeyDigest` check. Existing instrumented tests in `core:database` already
  cover the underlying "wrong key throws" assertion; this task wires the UI to surface
  it to the user.

## Unreleased — README accuracy update (2026-09-03)

- Updated the CI description in `README.md`: it previously said instrumented
  database tests require a manual device/emulator; the workflow has run them
  automatically on a GitHub-hosted emulator since the KVM/report-upload fixes
  earlier today. Also noted the new changelog-entry gate step.
  `signed-release` remains disabled — no signing secrets or the
  `YONTE_SIGNED_RELEASE_ENABLED` repository variable are configured yet
  (confirmed via `gh secret list` / `gh variable list`); this is expected
  ahead of the project's first release, not a defect.

## Unreleased — fix missing Composable import after TASK 15 split (2026-09-03)

- `BiometricUnlockRoute.kt` and `QuickUnlockSetupRoute.kt` were missing
  `import androidx.compose.runtime.Composable` after the TASK 15 file split,
  causing `Unresolved reference 'Composable'` and a compile failure (CI run
  `33716738111`). Added the import to both files; `PinRoute.kt` and
  `PassphraseUnlockRoute.kt` already had it correctly.

## Unreleased — TASK 15: split AppLockGate.kt (2026-09-02)

- Split `feature/onboarding/.../AppLockGate.kt` (248 lines) into one file per screen:
  `QuickUnlockSetupRoute.kt`, `PinRoute.kt` (+ `PinFieldMode`), `BiometricUnlockRoute.kt`,
  `PassphraseUnlockRoute.kt`. Pure mechanical split, verified byte-identical string
  literals and unchanged `MainActivity.kt` imports. (commit `6d16e26`)

## Unreleased — TASK 14 follow-up fixes (2026-09-02)

- Fixed `YonteDatabase.kt`: removed a leftover `// changelog-gate-test` debug comment
  left behind by an incomplete revert, and restored
  `.fallbackToDestructiveMigrationOnDowngrade()`, which had been silently dropped in
  the same round of changes with no stated reason. (commit `6bd8a19`)

## Unreleased — CI changelog-gate verification

- Verified the new changelog-enforcement gate (`tools/check_changelog.py`) correctly fails a push that touches `core/` without a CHANGELOG.md entry (run `33683980747`), then correctly requires one for the revert itself (run `33684758101`). Test change fully reverted; no product behavior changed.

## Unreleased — CI and agent-environment fixes (2026-09-02)

- Fixed `.opencode/agents/{implementer,reviewer,tester}.md`: YAML front matter was
  missing its closing delimiter and, in a later manual edit, its opening delimiter;
  the `bash` permission catch-all used `""` instead of the documented `"*"` wildcard,
  which meant unmatched bash commands fell through to the default-allow behavior
  instead of being denied. All three files are now valid YAML with `mode: subagent`
  and `permission.bash["*"] = deny` confirmed via independent re-clone and
  `yaml.safe_load()`. (commit `38f1f5f`)
- Fixed CI: `Instrumented database security tests` failed on every run because the
  `ubuntu-latest` GitHub-hosted runner does not grant `/dev/kvm` access by default,
  forcing the Android emulator into slow software emulation that never finished
  booting (`ProbeKVM: user has no KVM permissions`, run `33604781380`). Added a step
  to enable the KVM udev rule before the emulator step. (commit `d36a1bb`)
- Added `Upload instrumented test report` (`if: always()`) so instrumented test
  failures are diagnosable from the actual JUnit report instead of the truncated
  Gradle console log. (commit `2bb010a`)
- Fixed `YonteDatabaseEncryptionTest.singletonCanBeClosedAndReopenedWithTheSameKey()`:
  its `runBlocking { ... }` expression body's last statement was
  `context.deleteDatabase(...)`, which returns `Boolean`, making the compiled method
  non-void. JUnit4 rejected the whole test class (`InvalidTestClassError: ... should
  be void`) before any test could run. Added an explicit trailing `Unit`.
  (commit `7260919`)
- **Result:** `Instrumented database security tests` passed for the first time with
  real emulator execution, confirmed independently via
  `gh run view 33678173736 --json jobs`, run `33678173736`, all 16 steps `success`.
  This closes the two open items from `HARDENING_AUDIT_REPORT.md` (now marked
  HISTORICAL).

## 1.6.0 — Yonte notes and tasks interaction

- Added a Yonte-native Quick Add bottom sheet with separate Note and Task entry paths.
- Kept task capture honest and local: a Task starts as a concrete checkable line inside the editor instead of exposing an unimplemented task database.
- Added a dedicated notes-and-tasks product direction based on the supplied reference documents while preserving the Yonte name, package, local privacy model, and architecture.

## 1.5.0 — Yonte 2026 visual redesign

- Rebuilt the notes home around a warm paper-inspired Yonte surface, larger hierarchy, quieter search, a real grid layout, and a single prominent New note action.
- Reduced card clutter by moving archive, delete, and pin actions into a contextual overflow menu while keeping pinned notes visually distinct.
- Rebuilt the editor as a borderless writing canvas with a large title, wide body, unified scrolling, IME-safe padding, and a quiet local-save status.
- Refined settings with a clearer visual identity, icon-led section cards, and a calmer full-screen flow.
- Added a written Yonte 2026 design direction and updated the Yonte Design skill to prevent fallback to generic Material styling.

## 1.4.0 — Knote-inspired UX adaptation with original Yonte implementation

- Reworked the editor flow with a calmer title/body layout and a horizontal plain-text action strip for headings, bullets, tasks, and dividers.
- Fixed new-note autosave to allocate a stable draft UUID before the first asynchronous save, preventing duplicate rows during fast typing.
- Replaced the settings dialog with a full-screen sectioned settings flow covering Appearance, Data & backup, and Updates.
- Moved settings into an isolated `:feature:settings` module; the app root now composes Notes and Settings without feature-to-feature dependencies.
- Added a short UX adaptation record documenting what was learned from Knote and what was intentionally not copied.
- Added unit coverage for concrete editor actions; architecture, database, update, and debug build gates pass.

## 1.3.0 — UX/UI polish and reliable draft autosave

- Added an Android-style right-side drawer in RTL with a cleaner home surface.
- Replaced the ambiguous list/grid icon toggle with explicit stateful List and Grid chips.
- Added reliable draft autosave after a short debounce and final save on Back, Cancel, Save, and screen disposal.
- Preserved one draft identifier after the first autosave to avoid duplicate notes.
- Removed the unused legacy home implementation.
- Added a dedicated Yonte Design skill and documented the 2026 design principles.

## 1.2.0 — Secure update center

- Added a manual update checker in Settings.
- Added a public metadata-only update feed; the source repository remains private.
- Added version comparison, minimum SDK validation, APK download, SHA-256 verification, and Android installer handoff.
- No background polling, analytics, account, or user-data upload is used.

## 1.1.0 — V2 flow foundation

- Added a workspace-oriented notes home with pinned and recent sections.
- Added tag chips derived from note content and lightweight tag filtering.
- Added a list/grid presentation preference for the notes surface.
- Improved note previews, relative timestamps, and empty states.
- Added working theme control and encrypted backup/restore actions in settings.
- Added a reference analysis document for `Yoslim1/knote`; no code or assets were copied.
- Added the V2 roadmap and quality gates.

## 1.0.0 — Core foundation

- Android-native Kotlin and Jetpack Compose application.
- Room-backed local notes repository with Android API 26 minimum.
- Arabic normalization and FTS5 capability with a safe fallback search path.
- Android share-to-note intent for incoming plain text.
- Android Keystore AES-GCM foundation and versioned backup envelope.
- Private GitHub release tag `v1.0.0`.
