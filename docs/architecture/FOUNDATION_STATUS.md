# Architecture Foundation Status

## Purpose

Separate accepted target architecture from current product implementation so reviewers do not mistake design decisions or Draft pull requests for shipped behavior.

For the dated branch, Issue, Pull Request, and CI snapshot, read [Project Status](../PROJECT_STATUS.md).

## Documented target areas

- feature sovereignty and bounded-context data ownership.
- global entity identity, revisions, provenance, and relationships.
- command/query/event integration and reliability rules.
- authentication, authorization, and key-wrapping separation.
- AI capability permissions, confirmation, context/memory, provider boundaries, and derived-data governance.
- backup/restore safety and recovery-key evolution.
- canonical error/failure behavior, privacy-safe observability, quality gates, and release governance.
- design-system, accessibility/localization, lifecycle/composition, and agent-governance rules.

These are target constraints and migration decisions. They do not claim the corresponding production architecture already exists.

## Current implementation boundary

- architecture-foundation contains governance documentation and the completed narrow fix for Issue #3; it is not a completed product-architecture migration.
- Issue #4 is still pending integration through Draft PR #29, so committed Room schema history is not yet part of architecture-foundation.
- Draft PR #28 is not treated as completed hardening until its independent review and final-SHA CI evidence are complete.
- Existing production coupling, including Notes presentation persistence leakage, Settings lifecycle/composition debt, and ScheduledBackupWorker infrastructure construction, remains visible until migrated through the defined phases.

## Active prerequisites

### Correctness and security

- Issue #15 — separate candidate-key derivation from authenticated-session commit and preserve fail-closed publication.
- Issue #16 — reduce immutable KDF secret copies only with byte-for-byte compatibility proof.
- Issue #19 — define and harden PIN lockout clock/threat semantics.
- Issue #20 — decompose LocalKeyManager incrementally where secret lifecycles have different ownership.

Issue #3 is closed; its result is recorded in [Current Security Findings](../security/reviews/CURRENT_FINDINGS.md).

### Evolution safety

- Issue #4 — commit the Room schema-export and migration-test baseline before database/schema evolution.
- Preserve backup compatibility and recovery evidence before high-impact data-platform evolution.

### Boundary migration

- Issue #7 — remove Notes presentation dependency on Room persistence types.
- Issue #6 — enforce semantic persistence boundaries only after the affected boundary has been migrated.
- Issues #9 then #8 — establish long-running Settings operation ownership before presentation lifecycle changes.
- Issues #14 then #10 — align automatic-backup authority before thinning the Worker boundary.

## Gate

[Foundation Gate #17](https://github.com/Yoslim1/Yonte/issues/17) remains open. Do not start Global Identity, outbox/event infrastructure, AI cross-feature platform work, or a new product domain before their migration, recovery, authorization, and verification prerequisites are satisfied.
