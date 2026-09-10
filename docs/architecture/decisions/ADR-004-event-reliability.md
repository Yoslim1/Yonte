# ADR-004: Event Reliability Without Event Sourcing

## Status

Accepted target architecture.

## Context

Future automations need reliable fan-out after state changes, while a mobile local-first app should avoid unnecessary event-sourcing complexity.

## Decision

Persist integration events only where reliability requires it. Use stable event IDs, idempotent consumers, bounded retry/backoff, and a transactional outbox when losing the event after a successful state commit would violate behavior.

## Consequences

- State mutation and required event registration can be atomic.
- Duplicate delivery is safe.
- Repeated failures become inspectable instead of infinite retry loops.
- Ordinary synchronous use cases remain direct commands/queries.

## Rejected

- Full event sourcing as the default persistence model.
- Best-effort fire-and-forget for critical integration events.
