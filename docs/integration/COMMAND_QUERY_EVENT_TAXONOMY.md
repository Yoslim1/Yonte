# Command, Query, and Event Taxonomy

## Purpose

Prevent the event bus from becoming an untraceable replacement for explicit application flow.

## Command

Requests a state-changing action from an owning capability/domain. The caller needs a defined success/failure result.

## Query

Requests information without changing owning domain state.

## Event

Announces a state change that has already completed. Used for notification/fan-out and eventual follow-up.

## Rules

- Do not use events for required synchronous validation.
- Do not mutate another feature's tables in response to convenience shortcuts.
- Events contain identity/revision/safe metadata by default, not protected content.
- Commands and queries are typed and narrowly scoped.

## Example

AI issues `CreateTask`; Tasks validates/creates; after commit Tasks emits `TaskCreated`.
