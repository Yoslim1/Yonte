# Event Reliability

## Purpose

Define delivery guarantees for future cross-feature events without adopting full event sourcing.

## Rules

- Every persisted/integration event has a stable event ID.
- Consumers are idempotent or track processed IDs.
- State change + event registration use a transactional outbox when losing the event would violate system behavior.
- Dispatch uses bounded retry/backoff.
- Repeated failures move to an inspectable dead-letter/failure state rather than infinite retry.
- Event payloads remain minimal and privacy-safe.

## Ordering

Only contracts that truly require ordering should declare it. Do not impose global ordering across unrelated features.

## Non-goal

Yonte is not an event-sourced system by default.
