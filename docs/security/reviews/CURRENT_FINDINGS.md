# Current Security Findings

## Scope

This register lists active security design and implementation debt for the architecture-foundation baseline. It is not a substitute for live branch, Pull Request, or GitHub Actions evidence; see [Project Status](../../PROJECT_STATUS.md).

## Resolved since the baseline audit

### SEC-001 — Biometric setup asynchronous key lifetime

[Issue #3](https://github.com/Yoslim1/Yonte/issues/3) is closed. The architecture-foundation head contains the focused fix that keeps the operation-owned session-key buffer valid until a terminal biometric callback, exposes it only to successful persistence, zeroes it once at terminal completion, and delivers a final result at most once.

This resolution does not authorize a broader authentication redesign or change the existing Keystore, Argon2, SQLCipher, or fallback behavior.

## Active P1 findings

### SEC-002 — Authentication versus wrapping boundary

General Keystore wrapping is intentionally not user-auth-gated. Narrow contracts must prevent callers from treating decryption ability as authentication.

### SEC-003 — Backup/session credential coupling

Scheduled backup currently reuses session-key lineage. Portable recovery semantics should move to backup-specific versioned credentials only after compatibility tests exist.

### SEC-004 — Authorization platform missing

Capability/resource/confirmation policy must exist before AI receives cross-feature access. Security owns authorization decisions and policy evaluation; feature/domain contracts own semantic capability vocabulary and resource semantics, while global entity identity remains a data/platform contract.

### SEC-008 — Candidate key cached before database validation

Candidate-key derivation must remain separate from authenticated-session commit so an invalid candidate never becomes session state. This is tracked by Issue #15. Draft PR #28 does not close this finding until independently reviewed and verified at its final commit.

## Active P2 findings

### SEC-005 — KDF immutable secret copy

Current KDF input conversion creates an avoidable immutable String. Reduce immutable secret copies only with byte-for-byte compatibility evidence.

### SEC-006 — LocalKeyManager responsibility hotspot

Session, PIN, backup cache, salt, and unlock-preference lifecycles are concentrated in one class. Split only behind lifecycle-specific contracts with behavior-preserving tests; do not perform a one-shot Security rewrite.

### SEC-007 — PIN lockout wall-clock dependency

Define the accepted clock-manipulation threat before changing the rate-limit implementation.

## Constraints

No plaintext fallback, destructive migration, incidental crypto rotation, or weakening of fail-closed behavior is allowed without explicit security review.
