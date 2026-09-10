# Backup and Migration Threats

## Scope

Malicious/corrupt backups, partial restore, lost recovery credentials, cryptographic migration, and database migration.

## Threats and controls

### Corrupt or malicious backup

Validate size, format/version, authenticated decryption/integrity, schema compatibility, and record structure before modifying live state.

### Partial restore

Restore through staging plus transactional commit (or equivalent atomic strategy). Existing live data remains untouched on validation failure.

### Lost automatic-backup credential

Target design separates device-local wrapping from portable recovery using a versioned backup master/recovery-key model.

### Security migration failure

Legacy-read/current-write window. Verify replacement material before retiring old recoverable material.

### Database migration failure

Explicit migrations, committed schema history, migration tests, no destructive production fallback.

## Verification

Fault injection must cover truncation, tampering, wrong credential, unsupported versions, disk errors, process interruption, and post-restore validation failure.
