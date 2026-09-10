# Changelog

## Unreleased — P0: complete key lifecycle cleanup paths (2026-09-10)

- `app/.../MainViewModel.kt`: clear onboarding return values, track and cancel PIN submissions, isolate each unlock warm operation by lifecycle generation, close the protected database on ViewModel teardown, and clear pending PIN state during invalidation.
- `app/.../YonteAppModule.kt`: clear the database provider's caller-owned session-key buffer after database acquisition.
- `app/src/test/.../MainViewModelTest.kt`: install a test Main dispatcher so asynchronous unlock lifecycle tests run deterministically. (implementation commits: 705994d, 57c2ced, 6ab2c19, d9caada, 491dd6d, 6ccee05)

## Unreleased — P0: close lifecycle review findings (2026-09-10)

- `app/.../MainActivity.kt`: force the database warmer to execute a protected query so SQLCipher opening and migration failures are observed before the app remains unlocked.
- `app/.../MainViewModel.kt`: guard authentication and warming jobs with a lifecycle generation, cancel stale work during invalidation, and prevent a late job from clearing a newer session.
- `core/database/.../NoteRepository.kt`, `app/.../YonteAppModule.kt`: resolve the Hilt repository against the active database singleton so lock/unlock can safely close and reopen the protected database.
- `app/.../MainViewModelTest.kt`, `.github/workflows/android.yml`: correct the app test fixture/assertion and include `:app:test` in CI. (implementation commits: 4e620ff, d3e9922, cdf5ae7, 0a9bd30, 17b664d, 13276f7)

## Unreleased — P0: harden session key ownership and cleanup (2026-09-10)

- `app/.../MainViewModel.kt`: derive passphrase candidates without caching them before protected database validation; clear candidate, PIN, and backup key buffers on completion; fail closed on database warm failures; and add explicit session invalidation that closes the database and clears the interactive cache.
- `core/security/.../LocalKeyManager.kt`: `unlock()` now derives a caller-owned candidate without committing it to session state.
- `app/src/test/.../MainViewModelTest.kt`, `core/security/src/test/.../LocalKeyManagerTest.kt`: add coverage for failed database warming, session invalidation, and uncommitted candidate keys. (implementation commits: a80243b, 766a702, 0df51f3, bffcb04)

## Unreleased — Run foundation PRs through Android CI (2026-09-09)

- `.github/workflows/android.yml`: the pull-request trigger now covers
  `architecture-foundation` alongside `main`, allowing Issue #3 fix head
  `7970e82` to obtain the required canonical Android CI evidence without
  changing any job, step, or check.

## Unreleased — P0: preserve biometric enrollment key until terminal callback (2026-09-09)

- `app/.../BiometricEnrollmentOperation.kt`, `app/.../MainActivity.kt`: keep the
  `cachedSessionKey()` buffer alive across asynchronous biometric enrollment,
  zero it exactly once at terminal completion, and ignore duplicate terminal
  callbacks. Persistence failures and synchronous setup/start failures report
  enrollment failure without changing existing PIN/passphrase fallback behavior.
  (implementation commit: `7970e82`)

## Unreleased — restore SettingsViewModel CI fixture contract (2026-09-09)

- `feature/settings/src/test/.../SettingsViewModelTest.kt`: stub `LocalKeyManager.unlockMethod()` to the production default passphrase method in the shared fixture, preventing Mockito `null` from violating the non-null `SettingsUiState.unlockMethod` contract and allowing the existing backup-frequency tests to exercise their intended behavior. Production behavior is unchanged. (tracking: issue #2)

## Unreleased — TASK 25: Fix PIN main-thread freeze, biometric unlock stuck state, secure storage hardening (2026-09-08)

- `app/.../MainViewModel.kt`: `submitPin()` now runs Argon2id KDF on `Dispatchers.Default`
  instead of the main thread, eliminating a 1-2 second UI freeze on every PIN entry.
  Added busy-guard with PIN zeroing to prevent re-entrant submission.
- `app/.../MainActivity.kt`: biometric unlock method preference is only persisted after
  `BiometricPrompt` setup cipher round-trip succeeds; added null-cache recovery path
  for missing `cache_iv`/`cache_data` so users cannot get stuck in a dead-end state.
- `core/security/.../BiometricUnlockManager.kt` (new): extracted biometric key-wrap/unwrap
  logic from `MainActivity` into a reusable class with `BiometricCipherProvider` interface
  for testability; `MainActivity` no longer touches biometric SharedPreferences directly.
- `core/security/.../BiometricUnlockManagerTest.kt` (new): unit tests covering storage
  round-trip, missing-key handling, and key clearing via a fake JVM cipher provider.
- `feature/settings/.../SettingsRoute.kt`: choosing "Biometric" or "PIN" in Settings now
  triggers real setup (BiometricPrompt or PIN-creation screen) before the preference changes.
- `core/security/.../AppPinManager.kt`: PIN hash comparison uses constant-time
  `MessageDigest.isEqual` instead of `contentEquals`.
- `ROADMAP.md`: added deferred "plain SharedPreferences, values independently encrypted,
  migrate to DataStore+Tink if justified" item.

## Unreleased — TASK 24: Settings UI for unlock method configuration (2026-09-07)

- Added Security section to Settings allowing users to view and change their unlock
  method (passphrase, PIN, or biometric) after onboarding.
- Settings screen now shows the current unlock method and lets users switch between
  methods with clear descriptions.
- Relevant key caches are cleared when switching away from a method.
- Reuses existing onboarding UI patterns for consistency.

## Unreleased — TASK 23: Data flow hardening — backup integrity, FTS sync, backup lifecycle (2026-09-07)

- `core/backup/.../BackupService.kt`: added `isArchived` and `isTrashed` fields
  to `BackupNote` with `false` defaults for backward-compatible import of old backups;
  `buildNotesPayload` now writes both keys; new `parseBackupNote` helper reads
  them with `optBoolean` fallbacks; `readEnvelope` delegates to `parseBackupNote`.
- `core/backup/.../ScheduledBackupWorker.kt`: backup payload now includes the
  note's archive and trash state.
- `feature/settings/.../SettingsViewModel.kt`: export and import paths now carry
  `isArchived` / `isTrashed` through `BackupNote` ↔ `NoteEntity` conversion
  instead of hard-coding `false`.
- `core/backup/.../BackupService.kt`: added `MAX_BACKUP_FILE_BYTES` (16 MiB)
  constant and `readBackupBytes` bounded-read helper; `readEnvelope` uses
  `readBackupBytes` instead of unbounded `readBytes()`; `buildEncryptedEnvelope`
  rejects envelopes exceeding the size limit before writing.
- `core/backup/.../BackupCodec.kt`: `decrypt` now validates fixed-width header
  fields (salt and IV lengths) before invoking the expensive Argon2 KDF,
  replacing the looser range check from TASK 22; oversized payloads are rejected
  before any key derivation.
- `core/database/.../NoteRepository.kt`: `setPinned`, `setArchived`, `setTrashed`,
  `save`, and `restore` are now wrapped in `database.withTransaction` with FTS
  re-sync, preventing stale search results after archive/unarchive/trash/restore;
  added `observeAll()` method.
- `core/database/.../NoteDao.kt`: added `observeAll()` query for future
  collection views sharing a single stream.
- `feature/settings/.../SettingsUiState.kt`: added `isBackupBusy` field.
- `feature/settings/.../SettingsViewModel.kt`: export and import guard on
  `isBackupBusy` to prevent concurrent operations; key material is zeroed even
  on cancellation via `CoroutineStart.UNDISPATCHED` + `finally` blocks.
- `feature/settings/.../SettingsDataSection.kt`: Export and Import buttons are
  disabled while `isBackupBusy` is true; shows "Processing backup…" indicator.
- `core/backup/src/test/.../BackupCodecTest.kt`: added tests verifying that
  malformed headers and oversized payloads are rejected before key derivation.
- `core/backup/src/test/.../BackupImportLimitTest.kt` (new): tests for
  `readBackupBytes` bounded read accepting valid input and rejecting oversized
  input.

## Unreleased — Memory hygiene, payload validation, permanent-failure handling (2026-09-06)

- `feature/settings/.../SettingsViewModel.kt`: zero `sessionKey` and `localSalt`
  after use in `export()` — the `finally` block previously had an incorrect comment
  claiming the array was not a local copy; `LocalKeyManager.cachedSessionKey()` returns
  a fresh `ByteArray` on every call that was never zeroed.
- `core/security/.../AppPinManager.kt`: zero `actual` and `expected` byte arrays in
  `verify()` after comparison, preventing Argon2-derived hashes from lingering in
  memory.
- `core/backup/.../BackupCodec.kt`, `core/security/.../EncryptionManager.kt`: validate
  length prefixes before allocating salt/IV byte arrays in `decrypt()`; a corrupted
  payload with a garbage length now throws `IllegalArgumentException` quickly instead
  of attempting an unbounded allocation.
- `app/.../MainViewModel.kt`: zero `createdPin` in `onCleared()` so a PIN-creation
  flow abandoned via ViewModel teardown (back navigation, config change) does not
  leave the partial PIN in memory until GC. (Hard process kill cannot be intercepted;
  this narrows the window.)
- `core/database/.../DatabaseUtils.kt`, `core/backup/.../ScheduledBackupWorker.kt`,
  `app/.../MainViewModel.kt`: moved `isDatabaseVersionMismatch` classifier to
  `core:database` so both `ScheduledBackupWorker` and `MainViewModel` share it;
  `ScheduledBackupWorker` now returns `Result.failure()` for database-version-mismatch
  errors instead of retrying forever on a permanently unrecoverable condition.
- `app/.../MainActivity.kt`: detect `KeyPermanentlyInvalidatedException` (new
  fingerprint enrolled or device credential changed) on the biometric unlock path and
  permanently switch the unlock method to PIN or passphrase, preventing the biometric
  screen from being shown on every cold start when it can never succeed again.

## Unreleased — Fix `exceptionOrNull()` call syntax in database version mismatch handler (2026-09-06)

- `app/.../MainViewModel.kt:248`: corrected `exceptionOrNull` (property access) to
  `exceptionOrNull()` (function call) in `onUnlocked()`, fixing a Kotlin compilation
  error introduced in the previous commit.

## Unreleased — Remove destructive downgrade fallback; block on database version mismatch (2026-09-05)

- `core/database/.../YonteDatabase.kt`: removed
  `.fallbackToDestructiveMigrationOnDowngrade()` so a downgrade (older APK over a
  newer encrypted database) can never silently wipe user data; the FTS5 `onCreate`
  callback is unchanged. `YonteDatabase.close()` is now defensively safe (null /
  already-closed guard, never throws) so mismatch paths can always clean up.
- `app/.../MainViewModel.kt`, `app/.../MainUiState.kt`, `app/.../MainActivity.kt`:
  Room missing-migration `IllegalStateException` ("A migration from ..." +
  "was required but not found", cause chain included) is now classified as a
  database version mismatch. Both `submitPassphrase` and the `onUnlocked` warmer
  close the database and raise blocking `isDatabaseBlocked` state instead of
  unlocking into crashes; `MainActivity` renders a blocking `DatabaseBlockedRoute`
  (EN + AR) telling the user to update while reassuring that notes were not deleted.
- `app/.../MainViewModelTest.kt`: added coverage for the `onUnlocked` warmer
  mismatch (blocking state, warming cleared) and for the mismatch classifier
  (direct match, wrapped cause, non-match rejection).
- `ROADMAP.md` (Data Layer): documented the final no-destructive-fallback policy
  and the forgotten-migration caveat (a version bump without its migration ships
  the blocking screen until a later update provides it).

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
