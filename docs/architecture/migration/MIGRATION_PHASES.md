# Architecture Migration Phases

## Purpose

Define dependency gates for moving current Yonte toward the target platform without forcing premature abstractions or unsafe schema/security changes.

## Planning rule

Phases describe **prerequisites**, not arbitrary calendar buckets.

Read-only analysis, design, or isolated preparation may happen early when useful, but production behavior must not assume a prerequisite that has not passed its gate. A later phase is never a reason to leave an earlier safety defect unresolved.

## Phase 0 — Baseline health

- Keep canonical CI green.
- Fix confirmed P0 correctness/security defects before broad refactors.
- Preserve current user data and behavior while establishing a trustworthy baseline.

**Gate:** no broad architecture migration while a relevant P0 correctness/security defect or unexplained CI failure remains open.

## Phase 1 — Governance and evolution-safety baseline

- Maintain architecture/security/data/backup ADRs, change classification, and Definition of Done.
- Configure and commit the current Room schema baseline and establish migration-test support before any database version increment.
- Preserve known backup-format fixtures/round-trip evidence needed to detect compatibility regressions during future schema work.
- Strengthen only automated rules that the current legitimate architecture can already satisfy.
- Do not activate a future-state rule merely to make the baseline intentionally fail before its migration exists.

**Gate:** no global-identity/outbox/relation schema evolution before schema history and migration verification are reviewable.

## Phase 2 — Boundary and composition hardening

- Remove Room/persistence entity leakage from feature presentation through feature-owned domain contracts and explicit persistence mapping.
- Correct lifecycle/composition hotspots where UI/Workers directly own infrastructure graphs or lifecycle-bound state incorrectly.
- Narrow dependency surfaces without creating generic domain/core dumping grounds.
- After each affected boundary is migrated, enable the corresponding semantic architecture guard so the old coupling cannot return.

**Rule:** migrate first, then enforce the new invariant in the same phase/change sequence. Never weaken a guard to preserve invalid coupling.

## Phase 3 — Backup and recovery hardening

- Make portable-backup credential semantics explicit and symmetric.
- Establish staged restore that validates before touching live data.
- Add verified backup generations/last-known-good semantics where justified.
- Define recovery-key lifecycle and backward-compatible backup-format evolution.
- Exercise corruption, interruption, wrong-credential, incompatible-version, partial-restore, and storage-failure paths.

**Gate:** recovery behavior must be understood before introducing high-impact data/platform changes that would make old backups or rollback paths ambiguous.

## Phase 4 — Global identity and relationships

- Introduce stable global entity references, revisions, ownership/provenance, and relation semantics.
- Keep feature content owned by its bounded context; do not centralize all content into one global table.
- Apply versioned Room migrations with committed schemas and migration tests.
- Design identity so future sync/conflict handling remains possible without adding sync prematurely.

## Phase 5 — Authorization and consent

- Implement capability/resource/risk-based authorization before AI receives cross-feature access.
- Keep authentication, authorization, encryption, and confirmation as distinct concepts.
- Persist user-controlled AI feature capabilities explicitly and default sensitive access conservatively.
- Require confirmation based on operation risk even when capability authorization exists.

## Phase 6 — Commands, queries, and events

- Introduce typed commands/queries for explicit request-response integration.
- Use events for completed facts/fan-out, not as a substitute for every synchronous call.
- Add transactional outbox/idempotency only where atomic reliable delivery is genuinely required.
- Events carry references and non-sensitive metadata by default; protected content is fetched through permissioned owning contracts.

## Phase 7 — AI platform integration

- Add knowledge/action gateways, capability registry, provider boundary, context/memory governance, provenance, and derived-data invalidation.
- AI depends on semantic contracts and permissions, never feature persistence implementation.
- Writes use explicit commands and revision/conflict checks rather than direct database mutation.
- Failure of an AI/provider path must not reduce availability or integrity of core local features.

## Cross-cutting release/security work

Update trust, signer/package validation, secret/config hygiene, performance budgets, accessibility/localization, and release gates are applied when their affected surface changes. They are not postponed merely because they are not a numbered data-platform phase.

## Final rule

Do not skip a prerequisite because a later capability is more visible. Also do not block safe independent work solely because it appears in a later numbered phase; apply the relevant dependency and change gates instead of treating the phase list as bureaucracy.
