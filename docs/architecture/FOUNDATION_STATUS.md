# Architecture Foundation Status

## Purpose

Separate accepted target architecture from current production implementation so reviewers never confuse design intent with shipped behavior.

## Documented and accepted target areas

- feature sovereignty and data ownership.
- global entity identity/revisions/provenance.
- command/query/event integration taxonomy and reliability.
- authentication/authorization/key-wrapping separation.
- AI capability permissions, confirmation, context/memory, provider boundaries.
- backup restore safety and recovery-key target model.
- canonical errors, privacy-safe observability, quality gates.
- design-system, accessibility, localization, and agent governance rules.

## Current production reality

The branch is still documentation-only. Production code remains inherited from `main` and has not yet been migrated to these target contracts.

Known baseline blockers before broad refactoring:

1. `main` CI is red in `feature:settings` unit tests.
2. biometric setup has a confirmed asynchronous session-key lifetime defect.
3. Room schema export configuration is incomplete despite schema export being enabled.

## Gate

No large architectural refactor or new AI/event/data platform implementation starts before baseline correctness is restored and the relevant migration tests/contracts are ready.
