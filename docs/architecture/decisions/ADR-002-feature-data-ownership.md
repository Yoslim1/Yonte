# ADR-002: Feature Data Ownership

## Status

Accepted target architecture.

## Context

Yonte needs one coherent encrypted product while allowing Notes, Tasks, Calendar, Files, AI, and future features to evolve independently.

## Decision

Each bounded feature owns its domain semantics, persistence access boundary, models, invariants, and lifecycle rules. A shared encrypted physical database may host multiple feature-owned tables, but physical co-location does not grant cross-feature direct table access.

## Consequences

- Feature internals can evolve with limited ripple.
- Cross-feature access requires explicit contracts and stable entity references.
- Persistence entities must not become global application/domain models.
- Shared core types are limited to genuinely canonical platform concepts.

## Rejected

- One global repository/service owning all feature data.
- Separate database per tiny feature as a default rule.
