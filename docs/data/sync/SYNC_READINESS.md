# Sync Readiness Policy

## Purpose

Keep today's local-first data model compatible with a future optional sync engine without coupling current features to a server.

## Rules

- Local storage remains the source of truth unless a future ADR explicitly changes the model.
- User-owned entities use stable IDs that do not depend on one device/database row address.
- Mutable entities expose revision/version information sufficient for stale-write/conflict detection.
- Deletion semantics distinguish soft lifecycle state/tombstone from irreversible purge where future synchronization requires propagation.
- Conflict policy belongs to the owning bounded context; a global sync engine must not invent feature semantics.
- Sync transports references/contracts and feature-owned payloads through adapters; features do not depend on a specific cloud vendor.
- Offline creation/editing must remain valid when sync is unavailable.

## Non-goal

This policy does not authorize adding accounts, servers, telemetry, or synchronization today. It only prevents choices that make safe optional sync unnecessarily destructive later.
