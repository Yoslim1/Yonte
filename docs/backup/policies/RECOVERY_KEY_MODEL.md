# Backup Recovery Key Model

## Purpose

Define the target separation between automatic device-local backup and portable cross-device recovery.

## Target roles

### Backup master key

High-entropy random key used to protect backup payloads/envelopes. It is independent from ordinary interactive unlock credentials.

### Device-local wrapper

Android Keystore protects the backup master key for unattended scheduled backups on the current device.

### Recovery capsule

Portable, versioned encrypted copy of recovery material protected by a user-controlled recovery credential suitable for a new device.

## Rules

- Automatic backup must not require the user to type a passphrase every run.
- Loss/reset of device Keystore must not be the only reason historical portable backups become permanently unreadable.
- Recovery credential and app-unlock credential semantics are explicit and not accidentally overloaded.
- Never replace last known good recovery material until the replacement is verified.
- Envelope and recovery-capsule formats are independently versioned where needed.

## Migration constraint

Do not change current persistent backup semantics until old backups can be read and recovery/migration tests prove the transition.
