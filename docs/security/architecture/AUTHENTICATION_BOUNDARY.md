# Authentication Boundary

## Purpose

Define how Yonte proves that the expected user may open a protected session.

## Current mechanisms

- Passphrase-derived database key validated by successful protected database access.
- PIN verification with Argon2-derived comparison and lockout state.
- BiometricPrompt using a dedicated authentication-gated Keystore cipher.

## Boundary rules

- A decryptable wrapped blob is not proof of user presence by itself.
- Authentication failure never falls back to a default key or unlocked state.
- Biometric key invalidation falls back to another explicit authentication path.
- Candidate credentials should not become committed session state until validated.
- Session invalidation closes/forgets protected database access associated with that session.

## Non-goals

This boundary does not decide whether an authenticated actor may read/edit/delete a specific entity; that is authorization.

## Verification

Test state transitions: locked -> authenticated -> session, failed authentication, invalidated biometric key, and database-key mismatch.
