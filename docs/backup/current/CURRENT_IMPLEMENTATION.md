# Current Backup Implementation

## Scope

Facts confirmed from `core:backup` on the architecture-foundation branch.

## Current components

- `BackupGateway` / `BackupService` — manual import/export and envelope handling.
- `BackupCodec` — AES-GCM portable encryption using Argon2-derived or pre-derived keys.
- `ScheduledBackupWorker` — WorkManager backup to a user-selected document tree.
- `AutoBackupScheduler` — scheduling policy.

## Wire format

Current encrypted envelope uses `ynote-backup-encrypted`, `format_version = 1`, checksum metadata, and an encrypted JSON payload whose internal schema is versioned separately.

## Existing protections

- AES-GCM authenticated encryption.
- 16 MiB encoded-file limit before unrestricted parsing.
- format/schema markers.
- checksum verification after decryption.
- tests for codec round trips, wrong/corrupt inputs, size limits, frequency mapping, and worker behavior.

## Current coupling

Scheduled backup directly constructs/uses database, repository, security key manager, codec, filesystem API, and filename policy. This is a composition/lifecycle hotspot and should eventually depend on a high-level backup use case instead.

## Non-claims

The current implementation does not yet provide a complete staged restore, last-known-good generations, or portable recovery-key lifecycle.
