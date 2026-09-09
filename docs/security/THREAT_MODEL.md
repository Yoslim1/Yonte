# Yonte Platform Threat Model

## Status

Living threat model for the architecture-foundation effort. It distinguishes current protections from target controls and must be reviewed when security, storage, AI access, backup, sync, update, or recovery behavior changes.

## Security objectives

Yonte prioritizes:

1. Confidentiality of protected user data.
2. Integrity of user data and security state.
3. Recoverability without silent corruption or destructive fallback.
4. Explicit user control over AI access and high-risk actions.
5. Architectural containment so compromise or defects in one feature do not automatically grant direct access to another feature's internals.

Availability is important, but it must not be restored by weakening confidentiality or integrity.

## Primary protected assets

### Critical

- Database/session encryption keys.
- Recovery credentials and recovery capsules.
- Keystore-backed key material/aliases.
- Authentication secrets.
- Security and authorization policy state.

### Sensitive

- Note/task/calendar/file content classified as sensitive.
- Financial or identity-related user data.
- AI prompts/context containing sensitive user data.
- Portable encrypted backups before/after decryption.

### Private

- Normal user-created content.
- Entity relationships and metadata that reveal personal behavior.
- Search/index data.

## Actors

### Legitimate user

Owns the data and grants permissions.

### Local unauthorized person

Has physical access to an unlocked or locked device.

### Device/storage extractor

Can obtain application files or backup artifacts but is not assumed to possess unlocked in-memory keys.

### Malicious or compromised external AI/network provider

May receive data explicitly sent off device. Yonte must minimize disclosure and avoid treating an external provider as part of the trusted local boundary.

### Malicious/corrupted backup source

A supplied backup may be truncated, oversized, tampered with, incorrectly encrypted, structurally invalid, or crafted to exploit import logic.

### Supply-chain/update attacker

Attempts to replace or tamper with update metadata/APKs or dependencies.

### Faulty feature/agent implementation

Not necessarily malicious. Can accidentally cross boundaries, bypass intended contracts, log sensitive content, corrupt state, or introduce unsafe migrations.

## Trust boundaries

1. User interaction -> application authentication/consent boundary.
2. Feature UI/domain -> security and authorization contracts.
3. Feature/domain -> persistence contracts.
4. Encrypted application process -> filesystem/app storage.
5. Yonte -> external storage/backup destinations.
6. Yonte -> network/AI provider.
7. Application -> Android Keystore/biometric subsystem.
8. Update metadata/download -> installation trust decision.

Crossing a trust boundary requires validation appropriate to the data and operation.

## Current protection map

### Database confidentiality

Current design uses SQLCipher-backed Room and requires a passphrase-derived session key. There is no approved plaintext fallback.

### Passphrase and PIN derivation

Argon2id is used with per-secret salt. PIN verification uses constant-time `MessageDigest.isEqual` for derived-key comparison and implements escalating lockout state after repeated failures.

### Keystore wrapping

`EncryptionManager` wraps key material using Android Keystore AES-GCM. Its key intentionally does not require system user authentication and therefore is a wrapping/convenience primitive, not proof of user presence.

### Biometric path

A distinct AES-GCM Keystore key is configured with user authentication required and `AUTH_BIOMETRIC_STRONG` for every use.

### Known defense-in-depth gap

The KDF currently converts `CharArray` secrets through an immutable `String` before UTF-8 encoding. This does not break Argon2, but it creates an avoidable immutable secret copy and should be hardened.

## Threat scenarios and required controls

### T01: Stolen application storage

Threat: attacker copies SharedPreferences, encrypted database, and backup files.

Required controls:

- Database remains encrypted.
- Raw session keys are never stored.
- Wrapped-key caches must not be documented as authentication by themselves.
- Portable backups remain cryptographically protected.

### T02: Unauthorized cold-start access

Threat: protected data is opened before valid authentication.

Required controls:

- No database/repository initialization before session-key availability.
- Failure to obtain/unwrap an authorized key results in locked state.
- No fallback/default key.

### T03: PIN guessing

Threat: repeated local PIN guesses.

Required controls:

- Memory-hard PIN derivation.
- Escalating lockout/rate limiting.
- Constant-time verification comparison.
- Security tests for lockout transitions and successful-reset behavior.

Future hardening should consider whether device reboot/clock manipulation materially weakens lockout for the chosen threat model.

### T04: Biometric cache misuse

Threat: biometric-wrapped material is decrypted without fresh biometric authorization.

Required controls:

- Biometric-specific Keystore key with authentication required per use.
- Biometric cache and general wrapped cache remain separate concepts.
- Failure to initialize/decrypt the biometric cipher must not fall back to silent unlock.

### T05: AI overreach

Threat: AI reads or modifies data beyond user-granted scope.

Required controls:

- Capability-based permissions.
- Resource/feature scoping.
- Just-in-time confirmation for sensitive reads and destructive/high-risk actions.
- AI cannot call raw DAOs/Room tables.
- Authorization decisions are auditable without logging content.

### T06: AI stale-write conflict

Threat: AI reads revision N, user edits to N+1, AI overwrites the newer version.

Required controls:

- Entity revision/version checks.
- Optimistic concurrency for AI/automation writes.
- Conflict response instead of blind overwrite.

### T07: Sensitive data persistence in AI memory

Threat: one-time sensitive context becomes long-term AI memory.

Required controls:

- Context and memory are separate concepts.
- Sensitive context is non-persistent by default.
- Memory writes have an explicit policy/consent path.
- External-provider processing is visibly distinguished from local processing.

### T08: Event data leakage

Threat: event bus/outbox contains sensitive user content and becomes a secondary data store.

Required controls:

- Events contain IDs, types, revisions, timestamps, correlation metadata, and safe classification metadata.
- Events do not carry protected content unless a narrowly documented event contract proves it necessary.
- Consumers fetch content through authorized feature contracts.

### T09: Duplicate event execution

Threat: retries cause duplicate tasks, reminders, deletions, or AI actions.

Required controls:

- Stable event IDs.
- Idempotent consumers or processed-event tracking.
- Destructive commands must not be replayed blindly.

### T10: Event loss after successful write

Threat: process crashes after feature data commits but before event dispatch.

Target control:

- Transactional outbox for state change + event registration where atomicity is required.
- Retry/backoff and dead-letter handling for repeatedly failing consumers.

### T11: Corrupted or malicious backup

Threat: import damages live data or exhausts resources.

Required controls:

- File-size limits before expensive processing.
- Format/version validation.
- Authenticated decryption/integrity verification.
- Schema compatibility check.
- Parse/validate before modifying live state.
- Restore through staging/transactional strategy.
- Existing live data remains intact on failure.

### T12: Partial restore

Threat: half of a backup is restored before a failure.

Required controls:

- Transactional commit of validated restore set or equivalent safe staging strategy.
- Post-restore verification before declaring success.
- Conflict strategy must be explicit and testable.

### T13: Loss of automatic-backup credential

Threat: device/Keystore reset makes existing backups unreadable.

Target control:

- Decouple portable backup recovery from device-only wrapping.
- Use a versioned recovery design such as a high-entropy backup master key protected locally by Keystore and separately recoverable through a user-controlled recovery credential/capsule.
- Never overwrite the last known good recovery material until new material is verified.

### T14: Security migration failure

Threat: new crypto/key policy makes legacy encrypted material inaccessible.

Required controls:

- Version persistent security envelopes.
- Legacy-read/current-write migration window.
- Verify new wrapped/encrypted material before retiring old material.
- Migration failures preserve the recoverable old state.

### T15: Database migration failure

Threat: schema upgrade loses or corrupts encrypted user data.

Required controls:

- Explicit migrations.
- No destructive fallback for production data.
- Versioned Room schema exports and migration tests before schema evolution.
- Failure surfaces visibly and preserves source data.

### T16: Logging/diagnostic leakage

Threat: user content/secrets reach logs or crash diagnostics.

Required controls:

- Structured safe error codes.
- Entity IDs/correlation IDs instead of content.
- No raw prompts, notes, secrets, keys, PINs, passphrases, decrypted backup payloads.

### T17: Agent-induced boundary violation

Threat: coding agent implements a shortcut such as UI -> DAO, AI -> database, or feature -> feature internals.

Required controls:

- Architecture guard rules.
- Scope fence/impact report for non-trivial work.
- Contract change gate.
- Security/data change review.
- CI enforcement stronger than documentation alone.

### T18: Update trust compromise

Threat: attacker changes both update artifact and remote metadata describing expected hash/signer.

Required controls for the hardened target:

- Integrity hash validation.
- Expected package identity validation.
- Signer trust anchored independently from mutable remote manifest data, such as pinned/installed trusted signer policy.
- Fail closed on identity/signature mismatch.

## Recovery priority

When a failure occurs, recovery order is:

1. Preserve existing user data.
2. Preserve cryptographic recoverability.
3. Prevent unauthorized disclosure or mutation.
4. Restore consistent state.
5. Restore full availability.

Availability must never take precedence over data confidentiality/integrity by disabling protection.

## Testing implications

Critical security/recovery areas require more than happy-path unit tests.

Recommended classes of tests:

- Authentication state-transition tests.
- PIN lockout tests.
- Biometric wrap/unwrap contract tests.
- Encrypted database open/invalid-key tests.
- Migration tests.
- Backup corruption/wrong-password/old-version tests.
- Fault-injection tests for interrupted restore/migration.
- Authorization matrix tests for AI capabilities.
- Confirmation-policy tests for destructive actions.
- Event replay/idempotency tests.
- Secret/log scanning checks.

## Review triggers

This threat model must be re-reviewed when introducing or materially changing:

- AI/network providers.
- Sync/cloud functionality.
- New data domains.
- Authentication methods.
- Keystore/key-wrapping strategy.
- Database schema/storage engine.
- Backup/recovery format.
- Event infrastructure.
- Permission/consent model.
- Update/install trust path.

## Explicit non-claims

Yonte does not claim protection against every compromised/rooted runtime or a fully compromised operating system. The architecture should still minimize secret lifetime and cross-feature blast radius, but the device OS/Keystore remains part of the trusted computing base for current local protection.
