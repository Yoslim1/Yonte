# Architecture Migration Index

## Objective

Evolve current Yonte into the governed modular platform defined by the constitution while preserving existing behavior, encrypted data, and recovery guarantees.

## Documents

- `migration/MIGRATION_PHASES.md` — ordered implementation phases.
- `migration/CHANGE_GATES.md` — required safety gates before changes land.
- `CURRENT_ARCHITECTURE_AUDIT.md` — current-state overview.
- `DATABASE_ARCHITECTURE_AUDIT.md` — persistence boundary findings.
- `FEATURE_BOUNDARY_MAP.md` — current/target feature ownership boundaries.

## Rule

Migration is incremental and behavior-preserving unless a separately approved product change explicitly requires new behavior.
