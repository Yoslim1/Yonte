# Yonte Security Doctrine

## Status

Architecture policy for the `architecture-foundation` branch. This document defines security invariants and target boundaries. It does not claim that every target component is already implemented.

## Purpose

Security in Yonte is a sovereign platform capability. Features consume narrowly scoped security contracts; they do not own cryptography, key lifecycle, authentication policy, or authorization policy.

The doctrine must remain stable while security mechanisms evolve.

## Permanent doctrine

1. Default deny.
2. Least privilege.
3. User intent and consent are authoritative for user-facing actions.
4. Authentication and authorization are separate concerns.
5. Protected data must not be opened before an authenticated session is established.
6. No plaintext persistence of encryption keys or protected user content outside approved encrypted storage.
7. Secrets and sensitive content must not be written to logs, analytics, events, crash metadata, or diagnostics.
8. Sensitive operations fail closed.
9. Cryptographic primitives are replaceable behind stable contracts; features must not depend on algorithms or keystore details.
10. Security migrations must preserve recoverability and backward readability until old material is safely migrated.
11. Security-sensitive changes require focused tests, threat review, and human approval before production release.
12. No feature may bypass the security platform for convenience.

## Current security foundation

The repository already provides:

- Argon2id-based passphrase/PIN derivation.
- SQLCipher database encryption with no plaintext fallback.
- Android Keystore-backed AES-GCM wrapping.
- A biometric-gated Keystore key requiring `BIOMETRIC_STRONG` authentication for use.
- PIN lockout state.
- Wrapped session-key caches for convenience flows.
- Unit/instrumented security tests.

These capabilities are preserved during architectural migration.

## Trust and responsibility boundaries

### Authentication

Authentication answers: `Is the expected user presently authorized to open the protected session?`

Examples:

- Passphrase verification through successful database-key derivation/opening.
- PIN verification.
- BiometricPrompt with a biometric-gated cipher.

Authentication must not be inferred solely from possession of a decryptable local blob.

### Key wrapping

Key wrapping protects key material at rest. A wrapping key that does not require user authentication is not, by itself, proof of user presence.

Yonte must keep the concepts separate:

- `AuthenticationGate`
- `SessionKeyStore`
- `KeyWrappingService`

A feature must never use `can decrypt wrapped key` as its own authorization rule.

### Authorization

Authorization answers: `May this actor perform this capability on this resource now?`

The target authorization platform must support actors such as:

- User.
- AI Assistant.
- Scheduled system worker.
- Import/restore subsystem.

Authorization is capability-based and resource-scoped.

Example capabilities:

- `notes.read`
- `notes.create`
- `notes.update`
- `notes.delete`
- `tasks.read`
- `tasks.create`
- `files.read_sensitive`

No future AI feature receives implicit superuser access.

## AI security doctrine

AI is a powerful client of the platform, not a privileged bypass.

AI access must flow through:

`AI -> Permission/Consent Gate -> Feature Contract -> Domain/Repository -> Protected Storage`

Rules:

- AI must not access Room/SQLCipher tables directly.
- User-configured capabilities define standing permission.
- Risk-sensitive operations can require just-in-time confirmation even when a standing permission exists.
- Delete and destructive bulk operations require confirmation.
- Sensitive reads require explicit just-in-time confirmation unless a future policy explicitly defines a safer narrow exception.
- Temporary sensitive context must not automatically become AI memory.
- If a cloud AI provider is used, the UI must distinguish local-device processing from network processing before sensitive data leaves the device.
- Yonte must not promise deletion from an external provider's infrastructure unless that guarantee is contractually and technically verified.

## Risk classes

### Low

Examples: read non-sensitive item under an already granted permission, search metadata, create a low-risk item after an explicit user command.

### Medium

Examples: modify existing content, create multiple entities, cross-feature automation.

May require preview or confirmation according to impact.

### High

Examples: bulk modification, sensitive-content disclosure to an external processor, changing privacy/security settings.

Requires explicit confirmation and audit metadata.

### Critical

Examples: permanent deletion, recovery-key replacement, cryptographic migration, destructive data operation.

Requires strong confirmation, focused validation, and fail-closed behavior.

## Sensitive-data classification

Initial policy classes:

- `PRIVATE`: normal user content.
- `SENSITIVE`: financial, identity, private files, explicitly protected content.
- `CRITICAL`: encryption keys, recovery material, authentication secrets, security configuration requiring privileged handling.

`CRITICAL` material is never exposed to AI as ordinary knowledge content.

## Session-key lifecycle

The raw session key must exist only when required.

Rules:

- Never persist a raw session key.
- Wrapped caches must be purpose-specific.
- Callers that receive mutable key bytes must clear their copies when practical.
- Session invalidation must close/forget database access associated with that session.
- Background backup credentials must be separated from interactive application-session credentials in the target architecture.

## Memory hygiene

Mutable secret buffers should be zeroed when practical. Avoid creating additional immutable secret copies when APIs allow it.

Known hardening target: KDF input conversion should avoid unnecessary immutable `String` copies of passphrases/PINs.

This is defense-in-depth; Java/Kotlin runtime semantics mean complete memory erasure cannot be guaranteed.

## Crypto agility

Features must never depend directly on:

- AES-GCM implementation details.
- Argon2 parameter constants.
- Android Keystore aliases.
- SQLCipher key representation.

Security artifacts must support explicit versions where persistence compatibility matters, for example:

- key-envelope version.
- backup encryption-envelope version.
- security-policy version.

Migration pattern:

1. Read current and supported legacy versions.
2. Write only the current version.
3. Rewrap/re-encrypt safely when authenticated and appropriate.
4. Verify new material before discarding old recoverable material.
5. Remove legacy readers only after an explicit compatibility decision.

## Audit and diagnostics

Security audit records contain references and metadata only.

Allowed examples:

- actor.
- action/capability.
- entity ID/type.
- decision: allowed/denied/confirmation-required.
- policy version.
- timestamp.
- correlation ID.
- safe error code.

Forbidden:

- note/task/file content.
- passphrases/PINs.
- encryption keys.
- recovery secrets.
- plaintext sensitive AI prompts containing user data.

## Failure doctrine

Known failures receive typed errors and explicit recovery behavior.

Unknown failures must:

1. Preserve current user data.
2. Fail closed when security is uncertain.
3. Avoid silently substituting weaker protection.
4. Return a safe user-facing outcome.
5. Produce non-sensitive diagnostic metadata with a correlation ID where practical.

Never recover by:

- disabling encryption.
- using a default key.
- skipping authentication.
- accepting invalid integrity data.
- destructive database migration.

## Target internal security capabilities

These names describe responsibilities, not mandatory one-class-per-item implementation:

- `AuthenticationGate`
- `AuthorizationService`
- `ConsentPolicy`
- `KeyDerivationService`
- `KeyWrappingService`
- `SessionKeyStore`
- `SensitiveDataPolicy`
- `SecurityAuditSink`
- `RecoveryKeyService`

The implementation should remain cohesive and avoid premature module explosion.

## Security change gate

A change touching authentication, authorization, cryptography, keys, protected database opening, AI permissions, recovery credentials, backup encryption, or update trust must include:

1. Threat/impact analysis.
2. Compatibility analysis.
3. Failure scenarios.
4. Focused automated tests.
5. Independent review when available.
6. Human approval before merge/release.

## Non-goals

- Security through obscurity.
- Custom cryptography.
- Feature-specific copies of global security logic.
- Attempting to predict every theoretically possible failure instead of building safe unknown-failure behavior.
