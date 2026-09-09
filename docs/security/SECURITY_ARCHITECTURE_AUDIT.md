# Current Security Architecture Audit

## Scope

This audit records the security-relevant implementation state inspected on the `architecture-foundation` branch, which currently shares production code with `main`. It separates confirmed current behavior from future target architecture.

Reviewed areas include:

- `Argon2Kdf`
- `EncryptionManager`
- `LocalKeyManager`
- `AppPinManager`
- `BiometricGateCipher`
- `BiometricUnlockManager`
- `MainViewModel`
- `MainActivity` biometric/passphrase/PIN wiring
- existing security test inventory

## Overall assessment

The current security foundation is materially stronger than a typical local notes application. It already uses memory-hard derivation, encrypted database storage, Android Keystore wrapping, a strong-biometric-gated path, explicit lock state, and dedicated security tests.

The architecture should be hardened incrementally; it should not be replaced wholesale.

## Confirmed strengths

### S1 — No intended plaintext database fallback

The protected Room database is designed to open with the passphrase-derived session key through SQLCipher. The architecture must preserve this invariant.

### S2 — Argon2id derivation

`Argon2Kdf` uses Argon2id with a random 16-byte salt and a 32-byte output. Parameters are explicitly tuned for the current device/performance target.

### S3 — PIN derivation and comparison

PINs are not stored directly. A salted Argon2-derived value is stored, and verification uses `MessageDigest.isEqual`. Successful verification resets lockout state.

### S4 — Escalating PIN lockout

Repeated failed PIN attempts introduce escalating lockout delays.

### S5 — Purpose-separated biometric key

Biometric unlock uses a dedicated Android Keystore AES-GCM key configured with user authentication required and `AUTH_BIOMETRIC_STRONG` for every use.

### S6 — No silent biometric fallback to unlocked state

Missing/corrupted/invalidated biometric key material falls back to PIN or passphrase selection instead of silently unlocking.

### S7 — Cold-start session-cache clearing

`MainViewModel` clears the general session cache on non-first-run startup and establishes an explicit locked UI state before protected feature access.

### S8 — Security tests exist

The repository has focused tests for PIN behavior, KDF properties/timing sanity, biometric unlock manager behavior, and local key management.

## Findings

### F1 — HIGH — Biometric setup key buffer is zeroed before asynchronous success callback

`MainActivity.launchBiometricSetupPrompt` captures `sessionKey` in the authentication callback, starts asynchronous `BiometricPrompt.authenticate`, and then zeroes `sessionKey` in the surrounding `finally` block immediately after `authenticate` returns.

Because the callback occurs later, the callback can observe an already-zeroed array and persist an encrypted zero key instead of the actual database session key.

Impact:

- Biometric enrollment can appear to succeed while storing unusable key material.
- Subsequent biometric unlock can derive a wrong/zero session key and fail database access.
- This is primarily correctness/availability; the observed design fails closed rather than creating a plaintext or authentication bypass.

Required fix:

- Give the asynchronous biometric operation ownership of an independent key copy.
- Do not zero that operation-owned copy until the callback reaches a terminal path (success/error/cancel/exception).
- Ensure exactly-once cleanup for every callback/exception path.
- Add a regression test around asynchronous buffer lifetime where practical.

### F2 — MEDIUM — KDF creates avoidable immutable secret copy

`Argon2Kdf` converts `CharArray` to `String` before UTF-8 bytes.

Impact:

- The immutable `String` cannot be explicitly zeroed.
- This is memory-hygiene defense-in-depth, not a break of Argon2 security.

Required hardening:

- Replace avoidable `String` conversion with an encoding path that minimizes immutable secret copies, subject to Android/Kotlin API realities.
- Clear mutable byte buffers after KDF use when ownership permits.

### F3 — MEDIUM — Authentication and non-auth-gated key wrapping are easy to conceptually conflate

`EncryptionManager` intentionally creates a Keystore key with `setUserAuthenticationRequired(false)` and is used for general/session/PIN/automatic-backup wrapped-key caches.

This is valid as a wrapping/convenience primitive, but it must not be treated as proof of user presence.

Required architecture action:

- Introduce/narrow contracts so authentication decisions are made by authentication/authorization policy, not by callers observing that a blob can be decrypted.
- Document purpose-specific key caches and their threat assumptions.

### F4 — MEDIUM — PIN lockout relies on wall-clock time

PIN lockout uses `System.currentTimeMillis()` for lockout deadlines.

Impact:

- Manual/device clock changes can affect rate-limit timing.
- Severity depends on the local-device threat model and OS/device control available to the attacker.

Required hardening decision:

- Define the intended resistance to clock manipulation.
- If needed, design rate limiting using monotonic time during a boot session plus safely persisted cross-reboot state, rather than introducing an ad hoc fix.

### F5 — MEDIUM — Current `LocalKeyManager` owns too many credential-cache purposes

`LocalKeyManager` currently handles:

- passphrase salt metadata.
- general session-key cache.
- PIN unlock-key cache.
- automatic-backup key cache.
- unlock-method preference.

Impact:

- It is becoming a security hotspot with multiple independent lifecycle policies.
- Future recovery/AI/security growth would increase coupling.

Required target refactor:

Split responsibility behind narrow contracts such as:

- session credential storage.
- interactive quick-unlock storage.
- background-backup credential storage.
- unlock preference/policy.

Do not split merely by creating many classes; split by lifecycle/security responsibility.

### F6 — MEDIUM — Background backup currently shares application session-key lineage

Automatic backup key caching is derived from/copies the active database session key into a purpose-specific wrapped cache.

Impact:

- Background backup recoverability is coupled to the interactive local-key model.
- Device/Keystore loss and portable recovery semantics become harder to reason about.

Required target design:

- Introduce a backup-specific master/recovery-key architecture with explicit versioning.
- Keep device-local wrapping separate from portable recovery credentials.
- Migrate only after backup compatibility/recovery tests exist.

### F7 — LOW/MEDIUM — Wrong passphrase derivation is cached before database validation

`LocalKeyManager.unlock` derives and caches the candidate key before `MainViewModel` verifies it by opening/reading the encrypted database.

Impact:

- A wrong candidate key can temporarily remain wrapped in the general cache while the app remains locked.
- The next successful attempt overwrites it, and automatic-backup refresh is only invoked after successful unlock, so no confirmed data disclosure follows from the current flow.

Hardening option:

- Separate `deriveCandidateKey` from `commitAuthenticatedSessionKey` so unvalidated keys never become session state.

This is recommended because it clarifies the authentication invariant even if current behavior is not an exploit.

### F8 — ARCHITECTURE GAP — Authorization platform does not yet exist

Current security primarily answers application unlock/key protection. It does not yet implement the future capability/consent system required for AI and cross-feature actors.

Target:

- capability-based permission model.
- resource/feature scope.
- just-in-time confirmation policy.
- auditable decisions using IDs/metadata only.

This must be implemented before AI receives cross-feature data/action access.

### F9 — ARCHITECTURE GAP — Security artifact versioning/crypto agility is not yet a platform contract

Current algorithm/envelope details exist in implementation, but there is no general policy layer for legacy-read/current-write security migrations across all future persistent security artifacts.

Target:

- explicit envelope/policy versions where compatibility matters.
- verified rewrap/re-encryption before retirement of legacy material.

## Priority order

### P0 — Before additional biometric/security feature work

1. Fix F1 biometric asynchronous key-lifetime bug.
2. Restore/verify green security/settings CI after the recent unlock changes.

### P1 — Before AI integration

3. Establish authentication vs authorization contracts.
4. Implement AI capability/confirmation policy specification and tests.
5. Establish entity/revision identity contracts for safe scoped authorization.

### P1 — Before backup architecture expansion

6. Resolve manual-vs-scheduled backup credential semantics.
7. Design backup master/recovery-key lifecycle and compatibility format.
8. Add corruption/interruption/recovery tests before migration.

### P2 — Security hardening

9. Improve KDF secret-copy hygiene.
10. Clarify/split `LocalKeyManager` responsibilities by lifecycle.
11. Decide clock-manipulation requirements for PIN lockout.
12. Add versioned security-envelope migration policy.

## Migration constraints

- No security hardening change may introduce plaintext fallback.
- Do not rotate/re-encrypt live user data merely to improve code organization.
- Persistent-key or backup-format changes require backward compatibility and recovery tests first.
- Current biometric/passphrase/PIN fallback behavior must remain fail-closed.
- Changes to security behavior require focused CI evidence before merge.

## Conclusion

The correct path is evolutionary hardening, not replacement. Preserve the existing encrypted-database and Keystore foundation, fix the confirmed lifecycle bug first, then introduce narrower security contracts so future AI, backup, and feature integrations consume policy rather than key-management implementation details.
