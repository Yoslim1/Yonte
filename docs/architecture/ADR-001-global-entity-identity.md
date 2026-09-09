# ADR-001: Global Entity Identity

## Status

Accepted for future architecture evolution.

## Context

Yonte will contain multiple independent domains:

- Notes.
- Tasks.
- Calendar.
- Files.
- AI capabilities.

Features need to cooperate without depending on each other's internal implementation.

## Decision

Introduce a stable identity reference for user-owned entities.

An entity reference contains:

- Entity ID.
- Entity type.
- Ownership context.
- Revision information.

Features own their internal storage, but cross-feature communication uses references.

## Rules

Allowed:

- Requesting data through feature contracts.
- Linking entities through references.
- Tracking revisions.

Not allowed:

- Direct access to another feature's tables.
- Passing sensitive content through generic events.

## Consequences

Benefits:

- Better scalability.
- Safer AI integration.
- Cleaner feature boundaries.
- Easier future synchronization.

Tradeoff:

- Additional abstraction layer requiring disciplined implementation.
