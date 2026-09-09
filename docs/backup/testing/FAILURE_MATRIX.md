# Backup and Restore Failure Matrix

## Purpose

Define mandatory negative/fault-injection scenarios. Happy-path round trips are insufficient for a recovery system.

## Import/restore failures

- truncated file.
- tampered ciphertext/tag.
- wrong passphrase/recovery credential.
- invalid outer format.
- unsupported format version.
- invalid inner schema version.
- oversized input.
- malformed JSON/record fields.
- duplicate/conflicting entity IDs.
- stale revision conflicts.

## Storage/runtime failures

- destination unavailable.
- permission revoked.
- disk full / short write.
- process killed during backup write.
- process killed during restore staging.
- process killed during commit.
- database temporarily unavailable/locked.

## Key failures

- missing device wrapper.
- Keystore reset/invalidation.
- corrupt recovery capsule.
- legacy key-envelope version.

## Required assertions

For every destructive-capable failure test:

- pre-existing live data remains valid unless commit completed atomically.
- no plaintext fallback occurs.
- failure is classified with a safe error code.
- temporary sensitive buffers/files are cleaned where practical.
- retryability is explicit.
