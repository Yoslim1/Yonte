# Security Lifecycle Audit

**Status:** Discovery only  
**Baseline:** `architecture-foundation`  
**Audit date:** 2026-09-10  
**Implementation changes:** None

## Scope and evidence

This audit covers key management, biometric and authentication flows, unlock/session handling, cleanup, database initialization, encryption boundaries, and existing tests. The governing documents are `AGENTS.md`, `docs/FEATURE_ISOLATION_BLUEPRINT.md`, `docs/architecture/*`, and `docs/security/*`.

Inspected implementation files:

- `app/src/main/java/com/yonte/app/MainActivity.kt`
- `app/src/main/java/com/yonte/app/MainViewModel.kt`
- `app/src/main/java/com/yonte/app/BiometricEnrollmentOperation.kt`
- `app/src/main/java/com/yonte/app/YonteAppModule.kt`
- `core/security/src/main/java/com/yonte/core/security/LocalKeyManager.kt`
- `core/security/src/main/java/com/yonte/core/security/AppPinManager.kt`
- `core/security/src/main/java/com/yonte/core/security/BiometricUnlockManager.kt`
- `core/security/src/main/java/com/yonte/core/security/BiometricGateCipher.kt`
- `core/security/src/main/java/com/yonte/core/security/SessionKeyCipher.kt`
- `core/security/src/main/java/com/yonte/core/security/EncryptionManager.kt`
- `core/database/src/main/java/com/yonte/core/database/YonteDatabase.kt`
- `core/database/src/main/java/com/yonte/core/database/DatabaseUtils.kt`

Inspected tests and checks include the security JVM tests, `MainViewModelTest`, `BiometricEnrollmentOperationTest`, `YonteDatabaseEncryptionTest`, `tools/check_architecture.py`, and `.github/workflows/android.yml`.

## Current architecture

```text
                         :app composition root
                                  |
       +--------------------------+--------------------------+
       |                          |                          |
 MainActivity / MainViewModel  YonteAppModule          feature routes
       |                          |                    onboarding/settings/notes
       |                          |
       |              +-----------+-----------+
       |              |                       |
       |       LocalKeyManager          AppPinManager
       |              |                       |
       |       SessionKeyCipher       Argon2Kdf + PIN state
       |              |
       |       EncryptionManager
       |              |
       |       Android Keystore, non-auth-gated AES-GCM
       |
       +--> BiometricUnlockManager --> BiometricGateCipher
       |          |                         |
       |          +--> SharedPreferences   +--> auth-gated Android Keystore AES-GCM
       |
       +--> YonteDatabase.get(context, sessionKey)
                    |
             SQLCipher SupportOpenHelperFactory
                    |
              Room NoteRepository / DAO

:feature:* --> :core:* contracts/data APIs
:core:*    --> no :app and no :feature imports
```

The app composition root provides security and database services through Hilt. The platform `BiometricPrompt` remains in `:app`; security storage and cryptographic operations remain in `:core:security`. The repository and architecture documents describe the module direction as app to features/core, with core independent of app and features.

## Key ownership

| Material | Created by | Persisted form | Raw access | Result |
|---|---|---|---|---|
| Session/database key | `LocalKeyManager.setupPassphrase()` or `unlock()`, using `Argon2Kdf` | Wrapped AES-GCM payload in SharedPreferences; salt persisted separately | Returned to callers and supplied to `YonteDatabase.get()` | Plaintext is not persisted, but ownership is distributed across manager, ViewModel, and database opening |
| Session cache | `LocalKeyManager.cacheSessionKey()` / `cacheSessionKeyDirectly()` | Wrapped SharedPreferences value | `cachedSessionKey()` returns a fresh `ByteArray` | Caller must clear each returned copy |
| PIN verification material | `AppPinManager.setPin()` and `verify()` | Argon2 hash and salt in SharedPreferences | Comparison buffers are local to `verify()` | `actual` and `expected` are cleared in `finally` |
| PIN unlock database key | `LocalKeyManager.cachePinUnlockKey()` | Separate wrapped SharedPreferences value | Returned to `MainViewModel` during PIN unlock | Intentionally survives session-cache clearing |
| Biometric-wrapped key | Existing session key encrypted by `BiometricUnlockManager` | AES-GCM ciphertext and IV in SharedPreferences | Returned by `unwrapSessionKey()` after biometric authorization | Asynchronous enrollment lifetime is the highest-risk boundary |
| Automatic-backup key | `MainViewModel.refreshAutoBackupKeyCacheIfEnabled()` | Separate wrapped SharedPreferences value | Scheduled worker can retrieve it without UI | Intentionally survives interactive session clearing |
| Database digest | `YonteDatabase.get()` | In-memory SHA-256 digest | Used to bind singleton reuse to the active key | Cleared by `YonteDatabase.close()` |
| SQLCipher key copy | `YonteDatabase.get()` | Held by Room/SQLCipher open helper | Native/database layer | Kotlin cannot observe the native cleanup timing |

There are intentional mutable-array copies at KDF, cache-decryption, database-builder, and biometric boundaries. The active security documentation identifies avoidable immutable KDF input copies and the concentration of multiple secret lifecycles in `LocalKeyManager` as hardening targets.

## Current lifecycle

```text
Creation / derivation
  passphrase -> Argon2Kdf -> LocalKeyManager
  PIN        -> Argon2Kdf -> AppPinManager hash + salt
  biometric  -> Keystore-gated cipher wraps an existing session key
        |
        v
Usage
  MainViewModel establishes or receives a session key
  MainActivity/Hilt passes it to YonteDatabase.get()
  Room + SQLCipher use it for protected note access
  optional PIN, biometric, and backup caches hold wrapped copies
        |
        v
Expiration / invalidation
  cold start clears the interactive session cache
  wrong database key fails protected access
  biometric invalidation falls back to PIN/passphrase
  version mismatch blocks UI and closes the database
        |
        v
Cleanup
  input arrays are cleared in ViewModel finally blocks
  PIN comparison buffers are cleared in AppPinManager finally
  database instance and digest are cleared by YonteDatabase.close()
  pending created PIN is cleared in MainViewModel.onCleared()
  biometric enrollment operation clears its buffer on terminal completion
```

The `BiometricEnrollmentOperation` claims the first terminal callback and clears its owned buffer after success, failure, cancellation, synchronous exception, or duplicate terminal signals. The critical integration question is whether ownership is transferred to that operation before the asynchronous prompt can return.

Missing or weakly defined paths include candidate-key cleanup before database validation, complete cross-cache invalidation, cancellation at every coroutine boundary, non-version-mismatch database failures after `onUnlocked()`, and native SQLCipher key lifetime outside Kotlin observability.

## Database safety

1. **Pre-authentication initialization:** The normal Hilt provider is fail-closed: `provideDatabase()` calls `cachedSessionKey()` and throws when no session cache exists. Repository injection is lazy, and `MainActivity` warms the database only after unlock. `YonteDatabase.get()` itself accepts any caller-supplied `ByteArray`, so enforcement is at composition and flow boundaries rather than in the database API.

2. **Key handoff:** After onboarding or unlock, `MainViewModel` obtains a cached key and passes it to `YonteDatabase.get()`. The singleton computes a digest, replaces an instance bound to a different digest, and copies the key into the SQLCipher builder.

3. **Unlock enforcement:** The flow uses missing-cache checks, explicit UI state, lazy repository access, and protected-database validation. It is not a type-level capability: code with access to `YonteDatabase.get()` and a byte array can attempt to open the database.

The singleton correctly prevents reuse across different key digests and has an explicit defensive close path. The instrumented tests cover correct-key opening, wrong-key rejection, and close/reopen behavior. JVM and instrumented evidence remain distinct; this audit does not claim a current CI run.

## Architecture review

The documented feature/core dependency direction is preserved. Security implementation is isolated in `:core:security`, database implementation is isolated in `:core:database`, and platform biometric prompt construction is in the app composition layer.

Lifecycle responsibilities are concentrated in a few coordination points:

- `MainActivity` combines UI, platform biometric callbacks, unlock-method setup, and database-related navigation state.
- `MainViewModel` combines passphrase derivation, PIN state, biometric key receipt, database validation, warming, and backup-cache refresh.
- `LocalKeyManager` owns salt, unlock preference, session cache, PIN cache, and automatic-backup cache.
- `YonteAppModule` binds a singleton database to mutable session-cache state.

These are audit findings and Task 2 planning inputs. They do not justify a refactor during this discovery task.

## Findings

### SEC-LIFE-001 — Asynchronous biometric enrollment key ownership

**Issue:** A session-key array crosses an asynchronous biometric prompt and must remain intact until the first terminal callback.

**Severity:** Critical

**Evidence:** `MainActivity.launchBiometricSetupPrompt()` obtains `cachedSessionKey()` and starts `BiometricPrompt.authenticate()`; the callback executes later. `BiometricEnrollmentOperation` exists to own and clear that buffer. `docs/security/reviews/CURRENT_FINDINGS.md` records the same issue as SEC-001/P0.

**Impact:** The biometric cache can be written with zeroed or invalid key material, or a delayed callback can use a buffer after premature cleanup.

**Recommended solution:** In Task 2, make ownership transfer to `BiometricEnrollmentOperation` explicit before starting the prompt. Route every terminal callback through it and add delayed-callback, cancellation, synchronous-exception, and duplicate-terminal tests.

### SEC-LIFE-002 — Candidate key cached before database validation

**Issue:** `LocalKeyManager.unlock()` caches a derived candidate before SQLCipher proves it is correct.

**Severity:** High

**Evidence:** `unlock()` calls `cacheSessionKey(key)`; `MainViewModel.submitPassphrase()` then calls `YonteDatabase.get(...).noteDao().getAll()` to validate it. This is also recorded as SEC-008/P2.

**Impact:** An invalid candidate temporarily becomes shared session state and failure cleanup does not clearly evict every related cache.

**Recommended solution:** Separate derivation from authenticated-session commit. Validate first, then commit the session cache, with failure and cancellation coverage.

### SEC-LIFE-003 — No complete session invalidation contract

**Issue:** Session, PIN, biometric, automatic-backup caches, and database singleton have separate cleanup methods and different retention rules.

**Severity:** High

**Evidence:** `clearSessionCache()` removes only the interactive cache; PIN and automatic-backup caches intentionally remain. Biometric cleanup and `YonteDatabase.close()` are separate operations.

**Impact:** A future lock or key invalidation can clear one cache while leaving another usable or stale.

**Recommended solution:** Define and test explicit per-cache retention and one invalidation orchestration point. Preserve headless-backup retention only as an intentional, documented policy.

### SEC-LIFE-004 — Post-unlock warming can leave inconsistent state

**Issue:** `onUnlocked()` marks the UI unlocked and suppresses warmer failures except database-version mismatch.

**Severity:** High

**Evidence:** `MainViewModel.onUnlocked()` uses `runCatching`, classifies only `isDatabaseVersionMismatch`, and otherwise clears warming state without surfacing the exception.

**Impact:** SQLCipher, I/O, or lifecycle failure can leave the app presenting an unlocked state without usable protected data.

**Recommended solution:** Define fail-closed behavior for all database-open failures and cancellation paths. Close the database, clear session state as appropriate, and surface a recoverable locked/error state.

### SEC-LIFE-005 — Database access is boundary-enforced

**Issue:** `YonteDatabase.get()` accepts raw key material without a session capability.

**Severity:** Medium

**Evidence:** The Hilt provider and ViewModel enforce the normal flow externally; the database API itself accepts a `ByteArray`.

**Impact:** A future startup or dependency-graph change could open the database before intended unlock state.

**Recommended solution:** Tighten the existing boundary only as needed by a concrete session contract and test startup-before-unlock. Do not add a plaintext fallback.

### SEC-LIFE-006 — `LocalKeyManager` is a lifecycle hotspot

**Issue:** One class owns multiple independent secret and preference lifecycles.

**Severity:** Medium

**Evidence:** It owns KDF salt, session cache, PIN cache, automatic-backup cache, and unlock-method preference. This is recorded as SEC-006/P2.

**Impact:** Retention and invalidation rules can become coupled accidentally.

**Recommended solution:** Add lifecycle tests first. Split behind narrow contracts only if that reduces a demonstrated ambiguity.

### SEC-LIFE-007 — Backup key lineage is coupled to session key

**Issue:** Automatic backup stores a wrapped copy of the session key and permits headless scheduled use.

**Severity:** Medium

**Evidence:** `refreshAutoBackupKeyCacheIfEnabled()` calls `cacheAutoBackupKey(key)`; the cache survives `clearSessionCache()`. This is recorded as SEC-003/P1.

**Impact:** The scheduled worker has a longer-lived credential lineage than the interactive session.

**Recommended solution:** Treat backup-specific recovery credentials as a separate compatibility and migration project. Do not change the format during the lifecycle fix.

### SEC-LIFE-008 — Secret-copy ownership is inconsistent

**Issue:** Secret arrays cross several APIs without one uniform ownership contract.

**Severity:** Low

**Evidence:** KDF output, decrypted cache arrays, the database key copy, and biometric ciphertext buffers are handled by different owners. SEC-005/P2 records avoidable immutable KDF input copying.

**Impact:** Sensitive data may remain reachable longer than necessary and cleanup regressions may be hard to detect.

**Recommended solution:** Document ownership at narrow API boundaries and add focused observable cleanup tests. Do not claim JVM tests prove native memory zeroization.

## Existing test coverage

Security tests:

- `core/security/src/test/java/com/yonte/core/security/Argon2KdfTest.kt`
- `core/security/src/test/java/com/yonte/core/security/Argon2KdfTimingSanityTest.kt`
- `core/security/src/test/java/com/yonte/core/security/AppPinManagerTest.kt`
- `core/security/src/test/java/com/yonte/core/security/BiometricUnlockManagerTest.kt`
- `core/security/src/test/java/com/yonte/core/security/LocalKeyManagerTest.kt`

App unlock tests:

- `app/src/test/java/com/yonte/app/MainViewModelTest.kt`
- `app/src/test/java/com/yonte/app/BiometricEnrollmentOperationTest.kt`

Database and architecture checks:

- `core/database/src/androidTest/java/com/yonte/core/database/YonteDatabaseEncryptionTest.kt`
- `core/database/src/test/java/com/yonte/core/database/ArabicNormalizerTest.kt`
- `tools/check_architecture.py`
- `.github/workflows/android.yml`

Coverage is strongest for KDF behavior, PIN comparison/lockout, biometric persistence, wrong-key rejection, singleton close/reopen, and operation-level cleanup. Integration gaps remain around delayed platform callbacks, validation ordering, complete invalidation, non-mismatch failures, cancellation, and Hilt startup gating.

## Recommended Task 2 plan

1. Add focused lifecycle tests for biometric ownership, terminal callbacks, cancellation, and synchronous exceptions.
2. Fix explicit ownership transfer for the asynchronous biometric enrollment buffer.
3. Validate derived passphrase candidates against SQLCipher before committing session state.
4. Define cross-cache invalidation while preserving deliberate backup policy.
5. Make post-unlock database failures fail closed.
6. Add startup-before-unlock, wrong-key, key-switch, close/reopen, and failed-unlock integration tests.
7. Run architecture checks, security/database tests, and the authoritative GitHub Actions workflow, reporting instrumented evidence separately.

## Files expected to change in Task 2

- `app/src/main/java/com/yonte/app/BiometricEnrollmentOperation.kt`
- `app/src/main/java/com/yonte/app/MainActivity.kt`
- `app/src/main/java/com/yonte/app/MainViewModel.kt`
- `core/security/src/main/java/com/yonte/core/security/LocalKeyManager.kt`
- `core/security/src/main/java/com/yonte/core/security/BiometricUnlockManager.kt`
- `core/database/src/main/java/com/yonte/core/database/YonteDatabase.kt`
- Focused tests under `app/src/test`, `core/security/src/test`, and `core/database/src/androidTest`
- `CHANGELOG.md` when those impactful paths are changed

## Things that must not change

- Encryption algorithms, SQLCipher parameters, Argon2 parameters, Keystore authentication semantics, or persistent formats without a separate approved design and migration plan.
- Plaintext database fallback, pre-authentication database access, weakened biometric authentication, or treating general Keystore decryption as proof of user presence.
- Destructive migrations or silent data deletion.
- Feature-to-feature dependencies or core-to-app/feature dependencies.
- Unjustified new dependencies, abstractions, or broad refactors.
- UI changes beyond what is required to surface a fail-closed security state.
- Commits, pushes, merges, releases, or implementation PRs as part of this audit.

## Conclusion

The baseline has sound high-level separation: unlock paths establish a session key, normal Hilt database provisioning requires a wrapped session key, SQLCipher rejects wrong keys, and the singleton prevents reuse across different key digests. The highest-risk lifecycle boundary is asynchronous biometric enrollment. The next risks are candidate-key commitment before protected-data validation, incomplete cross-cache invalidation, and swallowed post-unlock database failures.
