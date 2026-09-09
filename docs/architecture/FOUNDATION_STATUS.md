# Architecture Foundation Status

## Purpose

Separate accepted target architecture from current production implementation so reviewers never confuse design intent with shipped behavior.

## Documented target areas

- feature sovereignty and bounded-context data ownership.
- global entity identity, revisions, provenance, and relationships.
- command/query/event integration and reliability rules.
- authentication/authorization/key-wrapping separation.
- AI capability permissions, confirmation, context/memory, provider boundaries, and derived-data governance.
- backup/restore safety and recovery-key evolution.
- canonical error/failure behavior, privacy-safe observability, quality gates, and release governance.
- design-system, accessibility/localization, lifecycle/composition, and agent-governance rules.

These are target constraints and migration decisions; they do not claim the corresponding production architecture already exists.

## Current implementation reality

- Production application source is still inherited from `main`; the architecture migration has not been applied to product code yet.
- The branch contains architecture/governance documentation plus one isolated test-fixture correction for `SettingsViewModel`.
- Issue #2 (red Settings unit-test baseline) is resolved with canonical CI evidence and no production contract weakening.
- Existing production coupling such as Notes presentation using Room persistence types, Settings lifecycle/composition debt, and ScheduledBackupWorker infrastructure construction remains intentionally visible until migrated through the defined phases.

## Active prerequisites

### P0 correctness/security

- Issue #3 — biometric enrollment asynchronous session-key lifetime. Broad production refactoring remains gated on this P0 being resolved through the repository's required independent implementation/review/verification workflow.

### Evolution safety

- Issue #4 — committed Room schema export and migration-test baseline before any database version/schema evolution.
- Backup compatibility/recovery evidence must remain reviewable before high-impact data-platform evolution.

### Boundary migration

- Issue #7 — remove Notes presentation dependency on Room persistence types.
- Issue #6 — enable semantic persistence-boundary enforcement after the affected boundary has been migrated, not before.

## Verification authority

This file intentionally does not pin a moving commit SHA or workflow run. The open foundation PR and GitHub Actions are the authority for the latest head/CI state. Never infer current CI status from an older documentation snapshot.

## Gate

Do not start Global Identity, outbox/event infrastructure, or AI cross-feature platform implementation before their migration, recovery, authorization, and verification prerequisites are satisfied.
