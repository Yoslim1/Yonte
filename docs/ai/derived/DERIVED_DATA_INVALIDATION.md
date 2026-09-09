# AI Derived-Data Invalidation

## Purpose

Prevent stale summaries, embeddings, memories, and classifications from surviving source changes unnoticed.

## Required metadata

Derived artifacts reference source entity ID and source revision/version.

## Rules

- Source update marks dependent derived artifacts stale unless the derivation explicitly remains valid.
- Source permanent deletion invalidates/removes dependent derived content according to retention policy.
- AI responses/actions should be able to expose provenance when useful.
- Recompute is asynchronous only when stale data cannot cause unsafe actions; high-risk actions require fresh source validation.

## Non-goal

Derived data is not canonical truth.
