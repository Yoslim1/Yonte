# AI and Event Threats

## Scope

AI authorization, external processing, stale writes, AI memory, event leakage, event loss, and replay.

## Threats and controls

### AI overreach

Capability-based, resource-scoped authorization; no direct DAO/Room access; high-risk confirmation.

### Stale AI write

Use entity revision/version checks and optimistic concurrency. Conflict instead of blind overwrite.

### Sensitive context persistence

Context and memory are separate. Sensitive context is non-persistent by default. Local vs external processing must be visible to the user.

### Event data leakage

Events carry identity/revision/metadata references, not protected content. Consumers fetch content through authorized contracts.

### Duplicate execution

Use stable event IDs and idempotent consumers/processed-event tracking.

### Event loss after commit

Use transactional outbox where state change and event registration must be atomic, with retry/backoff and bounded dead-letter handling.

## Non-goals

The event system is not a second user-content database and is not a replacement for synchronous commands/queries.
