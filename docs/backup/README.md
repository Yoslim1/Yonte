# Backup and Recovery Documentation

## Purpose

Index for Yonte backup/recovery architecture. Documents are split by responsibility.

## Documents

- `current/CURRENT_IMPLEMENTATION.md` — what production code does today.
- `policies/RESTORE_SAFETY.md` — invariants for safe restore.
- `policies/RECOVERY_KEY_MODEL.md` — target credential/recovery architecture.
- `testing/FAILURE_MATRIX.md` — required failure and fault-injection coverage.
- `reviews/CURRENT_FINDINGS.md` — prioritized implementation gaps.

## Principle

Backup success means recoverable, verified data—not merely a file write.
