# Key Lifecycle Architecture

## Purpose

Define ownership and lifecycle rules for database, wrapping, biometric, backup, and recovery keys.

## Key roles

- Database/session key: opens protected SQLCipher storage after authentication.
- General wrapping key: protects local cached material at rest; not proof of user presence.
- Biometric gate key: requires strong biometric authorization for each use.
- Backup master/recovery key: target capability for portable and scheduled backup recovery.

## Rules

- Raw keys are never persisted.
- Wrapped caches are purpose-specific and have explicit lifecycle owners.
- Mutable key copies are cleared when practical.
- Features never receive cryptographic implementation details.
- Persistent key/envelope formats are versioned when compatibility matters.
- Read supported legacy versions; write current version only.
- Verify new wrapped/encrypted material before retiring old recoverable material.
- Device-local wrapping and portable backup recovery credentials remain separate concepts.

## Current hardening targets

- Reduce `LocalKeyManager` responsibility by lifecycle, not by arbitrary class splitting.
- Avoid unnecessary immutable secret copies during KDF input encoding.
- Separate validated session establishment from candidate key derivation.

## Non-goals

No crypto rotation is performed merely for code organization.
