# Current Backup Findings

## Scope

Prioritized findings from the current backup implementation.

## P0/P1

### BAK-001 — Manual backup credential contract is inconsistent

`BackupGateway` documents an independent backup passphrase path, but current settings/export flow has also used the active session key + local salt path. The product contract must choose explicit semantics and expose distinct APIs such as portable vs scheduled export.

### BAK-002 — Scheduled worker constructs infrastructure directly

`ScheduledBackupWorker` creates/uses key manager, database, repository, codec, document storage, and naming policy. Target: worker depends on a high-level `BackupJob`/`AutoBackupRunner` composed through DI.

### BAK-003 — No staged restore transaction architecture yet

Current import returns parsed notes; safe staging/conflict/post-restore verification must be designed before broad restore evolution.

### BAK-004 — No last-known-good generation policy

A newest file is not necessarily a valid backup. Target policy should retain/identify verified generations and never discard the last verified recovery point before validating a replacement.

## P2

### BAK-005 — Derived key material cleanup

`BackupCodec` should clear mutable derived key bytes after crypto use where ownership permits.

### BAK-006 — Backup verification stops before re-open validation

Writing an envelope is not equivalent to proving the destination can be read back and authenticated. Define verification level for manual and scheduled backups.

## Constraint

Any persistent format/key change requires backward-read compatibility and recovery tests before migration.
