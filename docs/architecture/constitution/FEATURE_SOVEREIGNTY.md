# Feature Sovereignty

## Purpose

Define what each Yonte feature owns and what it may depend on.

## Ownership

A feature owns its domain semantics, presentation, use cases, feature-specific models, data access contract/implementation boundary, and tests.

Examples include Notes, Tasks, Calendar, Files, and AI.

## Rules

- Feature modules do not import other feature implementations.
- A feature never queries another feature's persistence tables directly.
- Internal UI, algorithm, provider, or storage changes should not ripple outside the feature unless an explicit public contract changes.
- Shared platform capabilities belong in narrowly scoped core contracts, not duplicated feature utilities.
- The app module is the composition root and orchestration boundary, not a business-logic dumping ground.

## Cross-feature access

Use explicit commands/queries for request-response interaction and events for notification/fan-out. Cross-feature entity references use stable identity rather than persistence classes.

## Verification

Architecture checks should reject feature-to-feature implementation dependencies and persistence-model leakage into unrelated presentation layers.
