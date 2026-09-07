# Baseline source audit — 2026-09-06

**Reviewed baseline:** `1a5cd9efe776d61f3e43faecc2d9da33ce93cb5e`.

This is a read-only source review of the baseline, not verification of the redesign currently in progress. All line numbers below refer to that SHA; use `git show 1a5cd9e:path` when the working tree has changed. Repository-relative links open the corresponding source files. A finding remains open in this report until a separate final-diff review establishes its disposition.

No Android build, device test, instrumentation test, accessibility assessment, real Argon2 measurement, or CI verification was performed by this review role. Severity describes impact and confidence in the source evidence, not demonstrated exploitation. The report is prioritized rather than exhaustive.

## Findings

### B01 — High — Unbounded backup parsing (observed)

Evidence: [BackupCodec.kt](../core/backup/src/main/java/com/yonte/core/backup/BackupCodec.kt), lines 29–33; [BackupService.kt](../core/backup/src/main/java/com/yonte/core/backup/BackupService.kt), lines 63–65.

`decrypt` allocates salt and IV arrays using untrusted encoded integers before validating remaining bytes or authenticating the payload. A short malformed file can request a very large allocation. The outer envelope is also read with unbounded `readBytes()`. Import is a user-selected file boundary; this is a local denial-of-service risk, not evidence of remote code execution.

Fix: bounded stream reading, version/format checks, fixed supported salt/IV lengths, minimum ciphertext/tag length, and remaining-byte validation before allocation or KDF. Preserve the existing valid wire format and cryptographic parameters. Add malformed-length and oversize regression tests.

### B02 — High — Backup loses archive/trash state (observed)

Evidence: [SettingsViewModel.kt](../feature/settings/src/main/java/com/yonte/feature/settings/SettingsViewModel.kt), lines 66–67 and 84–85; [BackupService.kt](../core/backup/src/main/java/com/yonte/core/backup/BackupService.kt), `BackupNote` and `buildNotesPayload`.

Export includes `getAll()` but the backup model omits archive/trash flags. Import forces both flags to false, resurrecting trashed notes and moving archived notes into the active list.

Fix: a separately reviewed backward-compatible format extension with explicit defaults for old backups and round-trip tests. This changes persistence semantics and must not be hidden inside cosmetic redesign or parser hardening.

### B03 — High — Import can overwrite newer local edits (observed behavior; intent unconfirmed)

Evidence: [NoteRepository.kt](../core/database/src/main/java/com/yonte/core/database/NoteRepository.kt), lines 32–33; [NoteDao.kt](../core/database/src/main/java/com/yonte/core/database/NoteDao.kt), `upsertAll`.

Restore uses unconditional `OnConflictStrategy.REPLACE` for matching IDs. Importing an old backup replaces newer local contents without conflict preview. No intended conflict policy was established by this review.

Fix: define and disclose restore semantics, preview collisions, and offer an explicit overwrite decision or preserve newer edits. Do not silently change the existing policy in a visual update.

### B04 — High — Expensive authentication and backup work on Main (observed)

Evidence: [MainViewModel.kt](../app/src/main/java/com/yonte/app/MainViewModel.kt), lines 163 and 183; [SettingsViewModel.kt](../feature/settings/src/main/java/com/yonte/feature/settings/SettingsViewModel.kt), lines 59–71 and 81–88; [Argon2Kdf.kt](../core/security/src/main/java/com/yonte/core/security/Argon2Kdf.kt), KDF parameters/comment.

PIN set/verify calls synchronous Argon2 directly from the UI callback. Backup import/export calls synchronous crypto and document streams in Main `viewModelScope` jobs. UI stalls follow from the execution path; their actual duration has not been measured here.

Fix: move CPU derivation to Default and stream work to IO, serialize submits, expose progress, and propagate cancellation. Keep key derivation parameters unchanged. Test completion, cancellation, and error cleanup.

### B05 — Medium — Restored notes missing from indexed search (observed)

Evidence: [NoteRepository.kt](../core/database/src/main/java/com/yonte/core/database/NoteRepository.kt), lines 32–33, 79, and 85–94.

Restore writes notes but never updates FTS. A successful FTS query with no indexed matches returns an empty list, so LIKE fallback does not rescue restored notes. Normal saves also update notes and FTS in separate operations.

Fix: synchronize index updates transactionally with mutations and cover restore/search behavior. Existing installations may need explicit index repair; testing only fresh databases is insufficient.

### B06 — Medium — Search results stale after mutations (observed)

Evidence: [NotesViewModel.kt](../feature/notes/src/main/java/com/yonte/feature/notes/NotesViewModel.kt), lines 27–32.

Nonempty search uses a one-shot flow, unlike the active-notes observable flow. Pin, edit, archive, and trash do not rerun the current query.

Fix: react to note-table invalidation while retaining query debounce and cancellation. Test trash/edit while a query remains unchanged.

### B07 — Medium — Clearing an existing note is discarded (observed)

Evidence: [NotesViewModel.kt](../feature/notes/src/main/java/com/yonte/feature/notes/NotesViewModel.kt), `autosave` and `saveImmediately`; [NotesScreen.kt](../feature/notes/src/main/java/com/yonte/feature/notes/NotesScreen.kt), `saveDraft`.

Blank title/body always returns without persistence or callback. Clearing an existing note leaves its old content in storage and can leave the UI reporting that it is saving.

Fix: distinguish a blank new draft from an existing note intentionally cleared; ensure terminal success/error UI state. Cover reopening a cleared existing note.

### B08 — Medium — Automatic-backup key lifecycle inconsistent (observed)

Evidence: [SettingsViewModel.kt](../feature/settings/src/main/java/com/yonte/feature/settings/SettingsViewModel.kt), `setAutoBackupDestination` and `setAutoBackupFrequency`; [ScheduledBackupWorker.kt](../core/backup/src/main/java/com/yonte/core/backup/ScheduledBackupWorker.kt), missing-key early return; [MainViewModel.kt](../app/src/main/java/com/yonte/app/MainViewModel.kt), `refreshAutoBackupKeyCacheIfEnabled`.

Enabling backup never populates its key cache; a newly enabled worker can report success without writing a backup until the next unlock. Conversely disabling clears the key, but the next unlock repopulates it because the refresh checks only destination, not OFF frequency.

Fix: populate only after authenticated, enabled configuration; keep OFF cleared; distinguish skipped work from successful backup. Test enable-without-restart and disable-then-unlock.

### B09 — Medium — PIN setup requires an unexplained second submit (observed)

Evidence: [PinRoute.kt](../feature/onboarding/src/main/java/com/yonte/feature/onboarding/PinRoute.kt), create-mode submit; [MainViewModel.kt](../app/src/main/java/com/yonte/app/MainViewModel.kt), lines 149–173.

The screen already compares PIN and confirmation, but the ViewModel treats the first valid submit as the first of another confirmation sequence without changing the visible stage.

Fix: one confirmation owner and one clear successful submit transition. Validate digits/length at the controller boundary, not merely via keyboard type.

### B10 — Medium — Settings ViewModel not lifecycle-owned (observed)

Evidence: [SettingsRoute.kt](../feature/settings/src/main/java/com/yonte/feature/settings/SettingsRoute.kt), `remember { SettingsViewModel(...) }`.

The manually constructed ViewModel is not registered with a ViewModelStore. Removal/recreation can leave an uncancelled scope and create another instance. Long-running callbacks retain the old screen context.

Fix: lifecycle-aware ViewModel factory/store ownership. Verify rotation, leaving during export/import, and returning to settings.

### B11 — Medium — Failed biometric enrollment committed as selected method (observed)

Evidence: [MainActivity.kt](../app/src/main/java/com/yonte/app/MainActivity.kt), setup chooser and `launchBiometricSetupPrompt`; [MainViewModel.kt](../app/src/main/java/com/yonte/app/MainViewModel.kt), `chooseBiometricUnlock`.

The method is persisted before successful wrapping. Cancellation, prompt errors, missing cipher, and encryption exceptions still call the same completion callback. Next startup can select biometric unlock without a usable cache. Passphrase fallback exists, so this is not a demonstrated permanent lockout.

Fix: persist biometric method only after successful authenticated wrapping and storage; expose recoverable failure/cancel state.

### B12 — Medium — Operation failures escape Result handling (observed)

Evidence: [SettingsViewModel.kt](../feature/settings/src/main/java/com/yonte/feature/settings/SettingsViewModel.kt), lines 87–92 and `downloadUpdate`.

`repository.restore` is called inside `Result.onSuccess`, outside the original `runCatching`; an exception skips intended failure UI. Installer launch has the same structure.

Fix: handle the complete operation in structured try/catch; rethrow cancellation; expose actionable errors. Test restore failure and missing/blocked installer activity.

### B13 — Medium — Startup failure handling incomplete (observed)

Evidence: [MainViewModel.kt](../app/src/main/java/com/yonte/app/MainViewModel.kt), lines 85–100 and `onUnlocked`; [MainActivity.kt](../app/src/main/java/com/yonte/app/MainActivity.kt), `setDatabaseWarmer`.

Onboarding does not catch setup failure and resets processing only on success. Warm-up discards failures other than migration mismatch. Its supplied warmer constructs a repository rather than explicitly querying the encrypted database, so it does not establish that storage opens successfully.

Fix: explicit startup error state, processing cleanup in finally, and an actual encrypted-database probe on IO before rendering notes. Preserve first-run/locked lazy initialization.

### B14 — Medium — Update certificate comparison lacks an independent trust anchor (conditional security risk)

Evidence: [UpdateService.kt](../core/update/src/main/java/com/yonte/core/update/UpdateService.kt), lines 110–111 and `certificateSha256`.

Downloaded signing certificate is compared to the same remote manifest supplying the APK. Package identity, installed signer/lineage, and archive version are not checked. If the update source were compromised, another package could be offered. Android still prevents replacing the installed Yonte package with an incompatible signer; no signing bypass is asserted.

Fix: validate package name, archive version, and installed signing identity/lineage independently. The existing manifest comparison must not be described as certificate pinning.

## Additional baseline UX observations

- `MainActivity.darkTheme` is transient and resets on recreation/cold start.
- Arabic detection is inconsistent: app treats any RTL layout as Arabic, while notes checks `Locale.language == "ar"`.
- Settings handles system Back only inside a subsection; at the root it exits the Activity rather than closing settings.
- The imported/shared draft initially reports “Saved locally” before a write has completed.
- Onboarding contains `طك حاجة`; backup copy contains `للStored notes`.
- Archived/trash list recovery, editor state on recreation, TalkBack focus, large-font clipping, contrast, and gesture conflict behavior need explicit final-design review and device validation.

## Existing protections observed

SQLCipher-backed Room, no plaintext database fallback, lazy dependency access before unlock, AES-GCM backup encryption, PIN lockout, HTTPS update origin restrictions, APK byte limit and checksum checks, nonexported FileProvider, and disabled Android automatic application backup are present in source. These observations do not establish complete security assurance.

## Verification and follow-up boundary

The safest bounded fixes without changing wire format or persistence semantics are parser bounds validation, moving existing work off Main with correct cancellation, lifecycle ownership, and fixing error propagation. Restore conflict policy and backup flag preservation require separate compatibility decisions. Final delivery must distinguish implemented fixes from these baseline findings and cite actual CI/device evidence where available.
