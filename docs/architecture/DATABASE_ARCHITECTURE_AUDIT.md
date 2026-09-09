# Database Architecture Audit Baseline

## Current Discovery

The project contains a dedicated core/database module.

Current structure:

- database module build configuration.
- source separation for main, unit tests, and android tests.

## Architectural Direction

The database layer is a platform capability, not a feature owner.

Feature modules own their domain rules.

## Rules

- No feature directly accesses another feature's tables.
- Database migrations must be versioned.
- Sensitive data requires security review.
- Backup and restore must preserve integrity.
- AI access must pass through permission-controlled contracts.

## Future Target

Feature Repository -> Database Gateway -> Storage

Cross-feature communication uses stable entity references and contracts.
