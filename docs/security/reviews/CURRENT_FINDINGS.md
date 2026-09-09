# Current Security Findings

## Scope

Implementation findings confirmed while auditing the current `main` production code inherited by `architecture-foundation`.

## P0

### SEC-001 — Biometric setup asynchronous key lifetime

`launchBiometricSetupPrompt` starts asynchronous biometric authentication but zeroes the captured session-key buffer when `authenticate()` returns. The later callback can observe zeroed key material and persist an unusable biometric cache.

Required fix: give the asynchronous operation its own key copy and clear it exactly once on terminal success/error/cancel/exception. Add regression coverage.

## P1

### SEC-002 — Authentication vs wrapping boundary

General Keystore wrapping is intentionally not user-auth-gated. Narrow contracts must prevent callers from treating decryption ability as authentication.

### SEC-003 — Backup/session credential coupling

Scheduled backup currently reuses session-key lineage. Portable recovery semantics should move to backup-specific versioned credentials after compatibility tests exist.

### SEC-004 — Authorization platform missing

Capability/resource/confirmation policy must exist before AI receives cross-feature access.

## P2

### SEC-005 — KDF immutable secret copy

Current KDF input conversion creates an avoidable immutable `String`; reduce immutable secret copies where practical.

### SEC-006 — `LocalKeyManager` responsibility hotspot

Session, PIN, backup cache, salt, and unlock preference lifecycles are concentrated in one class. Split behind lifecycle-specific contracts when behavior-preserving tests are ready.

### SEC-007 — PIN lockout wall-clock dependency

Define the intended clock-manipulation threat before changing the rate-limit implementation.

### SEC-008 — Candidate key cached before database validation

Prefer derivation followed by explicit authenticated-session commit so invalid candidates never become session state.

## Migration constraints

No plaintext fallback, no destructive migration, no incidental crypto rotation, and no change to fail-closed behavior without explicit review.
