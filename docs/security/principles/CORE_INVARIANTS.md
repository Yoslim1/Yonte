# Core Security Invariants

## Purpose

Define the security rules that every Yonte feature and platform component must obey.

## Scope

Applies to production code, migrations, background jobs, AI actions, backup/recovery, updates, tests, and coding-agent changes.

## Invariants

1. Default deny and least privilege.
2. Authentication and authorization are separate decisions.
3. Protected storage is never opened before an authorized session exists.
4. No plaintext persistence of encryption keys or protected user content outside approved encrypted storage.
5. Sensitive content and secrets never enter logs, analytics, events, crash metadata, or diagnostics.
6. Sensitive operations fail closed when security state is uncertain.
7. Features depend on stable security contracts, not algorithms, Keystore aliases, or raw key material.
8. No feature or agent bypasses security policy for convenience.
9. Security migrations preserve recoverability until replacement material is verified.
10. Security-sensitive changes require focused automated tests and independent/human review before release.

## Non-goals

- Security through obscurity.
- Custom cryptography.
- Feature-specific copies of global security rules.
- Weakening protection to recover availability.

## Verification

Architecture checks and focused security tests should make violations difficult to express and easy to detect.
