# Concurrency and Conflict Model

## Purpose

Prevent silent lost updates when users, AI, background work, restore, or future sync act on the same entity.

## Baseline model

Entities participating in cross-actor writes expose a monotonic revision/version.

A write derived from revision N should fail with a typed conflict when current revision is not N unless the operation explicitly supports merge semantics.

## Rules

- AI never blindly overwrites a newer user edit.
- Conflict handling is explicit: retry-with-reread, merge, user choice, or abort.
- Multi-record invariants use database transactions where atomicity is required.
- Event delivery can be eventually consistent; core entity mutations that require atomicity must not be delegated to an event chain.

## Future sync

Define local source of truth, conflict strategy, tombstones, clock assumptions, and merge policy before adding cloud sync.
