# Integration Contract Versioning

## Purpose

Define ownership and safe evolution of shared commands, queries, events, and canonical models.

## Rules

- Every shared contract has an owning domain/platform area.
- Prefer backward-compatible additive changes.
- Breaking changes follow expand -> migrate consumers -> deprecate -> remove.
- Persistent event formats include a schema/version discriminator where needed.
- Deprecation includes removal criteria, not an indefinite deprecated state.
- Integration/contract tests cover supported versions and critical consumers.

## Anti-pattern

Changing a shared interface and fixing compilation errors across the repository is not a migration strategy.
