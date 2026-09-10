# Data Lifecycle

## Purpose

Define lifecycle semantics so deletion, restore, AI memory, sync, and backups remain consistent.

## States

Features may expose domain-specific states, but must explicitly define semantics for active, archived, trashed, and permanently deleted data where relevant.

## Rules

- Archive is not deletion.
- Trash is recoverable until permanent-deletion policy applies.
- Permanent deletion requires explicit confirmation for AI/destructive automation.
- Derived data tied to a deleted source must be deleted, invalidated, or detached according to documented policy.
- Backup retention and restore may preserve historical copies; UI/privacy policy must not falsely imply physical erasure from every historical backup.
- Future sync must define tombstone/deletion propagation before implementation.

## Verification

Lifecycle transitions and recovery behavior require focused tests.
