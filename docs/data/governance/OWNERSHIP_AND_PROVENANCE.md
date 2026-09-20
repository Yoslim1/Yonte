# Data Ownership and Provenance

## Purpose

Define who owns data semantics and how Yonte records where information came from.

## Ownership

Each bounded feature owns its content schema, invariants, repositories, and interpretation.

## Provenance

Cross-feature/AI-derived information should retain enough metadata to identify:

- source entity reference.
- source revision.
- producing actor/process.
- creation/update timestamp.
- derivation type when applicable.

## Rules

- AI may fetch source content through authorized contracts using entity identity.
- Global registries/relations store identity and safe metadata, not arbitrary feature content.
- Deleting/changing a source can invalidate derived summaries, embeddings, memories, or links.

## Non-goal

Provenance is not a content-logging system.
