# Device and Authentication Threats

## Scope

Physical/local access, copied application storage, cold start, PIN guessing, and biometric misuse.

## Threats and controls

### Stolen application storage

Controls: SQLCipher remains encrypted; raw keys are never stored; wrapped blobs are not treated as authentication.

### Unauthorized cold-start access

Controls: no protected repository/database access before valid session; no default/fallback key.

### PIN guessing

Controls: memory-hard derivation, constant-time comparison, escalating lockout, tested reset behavior.

Open hardening question: resistance to wall-clock manipulation must be explicitly decided.

### Biometric cache misuse

Controls: dedicated Keystore key requiring `BIOMETRIC_STRONG`, separate biometric/general caches, explicit fallback to PIN/passphrase on failure.

## Trust assumption

Current protection trusts Android OS/Keystore. Yonte does not claim full protection against a fully compromised/rooted runtime.
