# Architecture Migration Phases

## Purpose

Define dependency gates for moving current Yonte toward the target platform without forcing premature abstractions or unsafe schema/security changes.

## Planning rule

Phases describe **prerequisites**, not arbitrary calendar buckets.

Read-only analysis, design, or isolated preparation may happen early when useful, but production behavior must not assume a prerequisite that has not passed its gate. A later phase is never a reason to leave an earlier safety defect unresolved.

Every deferred item is re-evaluated when an adjacent boundary changes. Keep the deferral only while implementing it now would add more risk/coupling than it removes.

## Phase 0 — Baseline health

- Keep canonical CI green.
- Fix confirmed P0 correctness/security defects before broad refactors.
- Preserve current user data and behavior while establishing a trustworthy baseline.
- Keep `main` protected from foundation experiments; architecture work stays reviewable on its dedicated branch until merge approval.

**Gate:** no broad architecture migration while a relevant P0 correctness/security defect or unexplained CI failure remains open.

Current principal blocker: biometric enrollment asynchronous session-key lifetime (#3).

## Phase 1 — Governance and evolution-safety baseline

- Maintain architecture/security/data/backup ADRs, change classification, and Definition of Done.
- Configure and commit the current Room schema baseline and establish migration-test support before any database version increment (#4).
- Preserve known backup-format fixtures/round-trip evidence needed to detect compatibility regressions during future schema work.
- Strengthen only automated rules that the current legitimate architecture can already satisfy.
- Do not activate a future-state rule merely to make the baseline intentionally fail before its migration exists.

**Gate:** no global-identity/outbox/relation schema evolution before schema history and migration verification are reviewable.

## Phase 2 — Boundary, session, and composition hardening

- Remove Room/persistence entity leakage from Notes presentation through the Notes-owned domain contract boundary and explicit persistence mapping (#7 / ADR-008).
- Separate candidate key derivation from authenticated session commit so invalid candidates never become session state (#15).
- Extract cohesive long-running Settings operation owners before changing presentation lifecycle semantics (#9 before #8).
- After operation lifetimes are explicit, give Settings presentation a real lifecycle owner (#8).
- Align automatic-backup key presence with actual enabled/configured state before extracting the Worker boundary (#14 before #10).
- Thin `ScheduledBackupWorker` into an OS adapter over a high-level backup job only after its data/key-policy dependencies are explicit (#10).
- Narrow dependency surfaces without creating generic domain/core dumping grounds.
- After each affected boundary is migrated, enable the corresponding semantic architecture guard so the old coupling cannot return (#6).

**Rules:**

- migrate first, then enforce the new invariant in the same phase/change sequence.
- never weaken a guard to preserve invalid coupling.
- do not combine unrelated crypto-format/key-rotation changes with responsibility extraction.

## Phase 3 — Backup and recovery hardening

- Make portable-backup credential semantics explicit and symmetric without breaking the existing wire format unless evidence requires a version change (#12).
- Establish staged restore that validates before touching live data.
- Add verified backup generations/last-known-good semantics where justified.
- Define recovery-key lifecycle and backward-compatible backup-format evolution.
- Exercise corruption, interruption, wrong-credential, incompatible-version, partial-restore, and storage-failure paths.

**Gate:** recovery behavior must be understood before introducing high-impact data/platform changes that would make old backups or rollback paths ambiguous.

## Phase 4 — Global identity and relationships

- Introduce stable global entity references, revisions, ownership/provenance, and relation semantics.
- Keep global entity identity in the data/platform boundary; Security consumes resource references but does not own entity identity (ADR-010).
- Keep feature content owned by its bounded context; do not centralize all content into one global table.
- Apply versioned Room migrations with committed schemas and migration tests.
- Design identity so future sync/conflict handling remains possible without adding sync prematurely.

## Phase 5 — Authorization and consent

- Implement capability/resource/risk-based authorization before AI receives cross-feature access.
- Security owns policy evaluation/confirmation; each bounded context owns its semantic capability vocabulary (ADR-010).
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

Apply when the affected surface changes; do not postpone these solely because they are outside a numbered data-platform phase:

- update signer/package trust and local signing-lineage anchor (#11 / ADR-009).
- KDF/secret memory hygiene only after byte-for-byte compatibility vectors exist (#16).
- PIN rate-limit clock hardening only after the intended clock-manipulation/reboot threat is explicit.
- secrets/config hygiene, performance budgets, accessibility/localization, and release gates.

## Final rule

Do not skip a prerequisite because a later capability is more visible. Also do not block safe independent work solely because it appears in a later numbered phase; apply the relevant dependency and change gates instead of treating the phase list as bureaucracy.
