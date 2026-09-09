# Threat Model Index

## Purpose

Entry point for Yonte threat analysis. Threats are split by trust boundary so each review remains focused.

## Threat domains

- `threats/DEVICE_AND_AUTH.md` — device storage, cold start, PIN, biometric.
- `threats/AI_AND_EVENTS.md` — AI overreach, stale writes, event leakage/replay.
- `threats/BACKUP_AND_MIGRATION.md` — malicious backups, restore failure, migration/recovery.

## Global recovery priority

1. Preserve user data.
2. Preserve cryptographic recoverability.
3. Prevent unauthorized disclosure or mutation.
4. Restore consistent state.
5. Restore availability.

Availability must never be restored by weakening confidentiality or integrity.
