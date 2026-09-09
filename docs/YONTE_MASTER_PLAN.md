# Yonte Master Vision & Execution Plan

## 1. Purpose

This is the durable master map for Yonte.

It answers four questions for any future engineer, reviewer, or coding agent:

1. What is Yonte ultimately intended to become?
2. Which architectural and product decisions have already been made?
3. In what dependency order should the platform evolve?
4. Where is the authoritative detail for each decision or subsystem?

This document is intentionally broad but not encyclopedic. It summarizes the whole direction and links to focused authoritative documents. It must remain understandable in one review session.

## 2. Authority model

Use the following source hierarchy when this plan and another repository artifact appear to disagree:

1. explicit current user requirement.
2. security/data-integrity invariants and `AGENTS.md` execution policy.
3. accepted Architecture Decision Records (ADRs).
4. Yonte Architecture Constitution and specialized security/backup/data doctrine.
5. actual current production code, build configuration, and CI state for implementation facts.
6. this Master Plan for product destination, migration order, and cross-domain orientation.
7. operational issues/PRs for live implementation status.

This file does not silently supersede an ADR. A durable architecture decision changes through a new or superseding ADR, then this file is updated to reflect the decision.

## 3. Product destination

Yonte is not intended to remain a notes application.

The long-term product is a **Secure Personal Intelligence Workspace**: a local-first personal platform where notes, tasks, calendar/events, files, future personal domains, automation, and AI cooperate without sacrificing user ownership, security, or feature independence.

Core product properties:

- local-first and useful without an account or server.
- private by default.
- encrypted sensitive data at rest.
- feature-rich without becoming a monolith of tangled implementation dependencies.
- AI deeply integrated, but permissioned and unable to bypass feature ownership/security boundaries.
- resilient backup/recovery as a first-class capability rather than an afterthought.
- English and Arabic/RTL treated as product-level requirements.
- Android remains the current product platform; future sync/cloud capability must not distort the local architecture before it is actually needed.

## 4. Product evolution direction

The current mature domain is Notes. Future product domains are added only after the architecture prerequisites relevant to them are ready.

Expected long-term capability families include:

- Notes and personal knowledge.
- Tasks and actionable work.
- Calendar/events and time-oriented context.
- Files/attachments and personal artifacts.
- Habits or other personal workflows when product value justifies them.
- Financial/personal sensitive domains only behind stronger privacy and authorization gates.
- AI Assistant as a first-class interaction surface over user-authorized capabilities.
- automation and cross-feature workflows built through contracts/events, not direct implementation coupling.

The list is directional, not a requirement to build every possible module. Product value and architecture readiness determine what ships.

## 5. Product experience and identity

Yonte should feel like one coherent product even when features have distinct internal UX.

Current visual/product direction:

- positioning: **Secure Personal Intelligence Workspace**.
- brand palette direction: Deep Navy `#071A33`, Cyan `#00C8FF`, Silver `#DCE7F5`.
- logo direction: layered stylized `Y`, expressing security, knowledge, tasks/services, and connected layers.
- current preferred UI concept is the original English concept; Arabic/RTL remains first-class and must be designed deliberately rather than mechanically mirrored.

Shared design laws include color/typography/spacing tokens, accessibility, navigation language, error/confirmation patterns, motion principles, and security affordances. A feature may have its own visual culture without creating a second design system.

See `docs/design/` for authoritative design-system and accessibility/localization rules.

## 6. Architectural model — modular personal platform

Yonte evolves as a **modular monolith / personal platform**, not as microservices inside an Android app.

Mental model:

```text
                    YONTE PLATFORM

          security / identity / recovery laws
                       |
        ---------------------------------
        |           |          |        |
      Notes       Tasks     Calendar    Files ...
        |           |          |        |
        -------- stable contracts -------
                       |
        commands / queries / events / AI
```

Features are sovereign bounded contexts ("islands/countries") under shared platform laws.

A feature owns its domain meaning, behavior, presentation, and data semantics. It may change its UI, implementation, search strategy, or storage adapter without forcing unrelated features to change.

High connectivity is allowed and expected. High coupling is not.

See:

- `docs/architecture/constitution/FEATURE_SOVEREIGNTY.md`
- `docs/architecture/constitution/PLATFORM_BOUNDARIES.md`
- `docs/architecture/decisions/ADR-002-feature-data-ownership.md`

## 7. Module and dependency strategy

The module graph should express real ownership boundaries, not architectural decoration.

Current shared infrastructure remains in focused `core` modules such as security, encrypted database infrastructure, backup, update, design system, and navigation.

The application module is the composition root. It wires implementations to contracts and owns top-level Android lifecycle/navigation orchestration; it should not accumulate feature business logic.

Feature modules own presentation and feature behavior. They do not depend on other feature implementation modules.

Domain contract modules are introduced only when they create a useful stable boundary. The preferred first Notes migration is:

```text
                 :domain:notes
                 /           \
                /             \
      :feature:notes       :core:database
                \             /
                 \           /
                     :app
                 composition root
```

`:domain:notes` may cohesively own small related contracts/models such as `Note`, `NotesRepository`, and a narrower backup-oriented port where justified.

Do **not** create a generic `core:domain` dumping ground.

Do **not** introduce `:data:notes` merely to match a textbook diagram. Extract a data module only when transaction/storage ownership can be moved cleanly and the new module provides concrete change isolation.

## 8. Documentation and file organization rule

Yonte optimizes for **cohesion and reviewability**, not one-responsibility-per-file literalism and not minimum file count.

A file may contain two or three small responsibilities when they share:

- the same architectural area.
- the same owner/reviewer.
- the same authority/lifecycle.
- the same normal reason to change.

Split when concerns develop independent ownership, independent review gates, different security/data risk, or the file becomes hard to understand safely.

Avoid both megafiles and fragmented micro-files.

See `docs/DOCUMENTATION_STANDARD.md`.

## 9. Data ownership and physical storage

One encrypted physical Room database may contain tables owned by several bounded contexts. Shared physical storage does **not** mean shared ownership or unrestricted table access.

Rules:

- each domain owns its schema semantics and repository contract.
- Room entities/DAOs are persistence implementation, not UI/domain contracts.
- presentation must not expose or depend on `@Entity`, DAO, SQLCipher, or raw SQL types.
- cross-feature references use stable global identity/reference contracts, not another feature's entity class.
- migrations are additive/safe where possible and never use destructive migration as a shortcut for user-owned data.
- committed Room schema history and migration tests are required before schema evolution beyond the current baseline.

Current confirmed debt: Notes presentation still depends on `NoteEntity`; this is tracked for migration before the semantic guard is enabled.

See `docs/data/`, ADR-001/ADR-002, and issues #4/#7/#6.

## 10. Global identity — "data passports"

Every durable cross-feature object should eventually have a stable identity independent of its persistence representation.

Canonical identity concepts include, where needed:

- stable entity ID.
- semantic entity type.
- owning/source bounded context.
- owner/workspace/user context.
- revision/version for conflict-safe writes.
- creation/update metadata.
- provenance/source reference.
- lifecycle/sensitivity metadata where appropriate.

The global registry/relationship layer should store identity and relationship metadata, not duplicate all protected feature content.

Cross-feature relationships use references such as `EntityRef` and relation records rather than direct entity/table coupling.

Revision checks support optimistic concurrency so AI or automation cannot silently overwrite newer user changes.

See `docs/data/identity/ENTITY_IDENTITY.md`, ownership/provenance docs, and ADR-001.

## 11. Integration contracts

Yonte uses three distinct integration forms:

- **Command** — explicit request to perform an action.
- **Query** — explicit request for information.
- **Event** — fact emitted after a state change has already completed.

Use commands/queries for synchronous request-response behavior. Use events for fan-out and decoupled reactions.

Do not use an event bus as a replacement for every call.

Critical reliable event delivery may use a transactional outbox only when atomicity between persisted domain change and event publication is genuinely required.

Events carry references and non-sensitive metadata by default ("references, not secrets"). Consumers retrieve protected content through the owning permissioned contract.

See `docs/integration/` and ADR-003/ADR-004.

## 12. Security doctrine

Security is a platform authority, not a feature utility bag.

Stable invariants:

- user owns their data.
- local-first/private-by-default.
- deny by default for sensitive capabilities.
- least privilege / least capability.
- no plaintext sensitive data at rest.
- features do not receive raw master-key authority.
- authentication, authorization, encryption/key wrapping, and action confirmation are separate concepts.
- sensitive cross-feature/AI access requires policy and user intent.
- failures are secure/fail-closed where security is involved.
- security contracts remain stable while cryptographic implementations/parameters can evolve through versioned envelopes and migrations.

Platform security capabilities may evolve into narrow services such as identity, authentication, authorization, key vault/crypto, privacy policy, audit, and update-trust verification. Avoid one omnipotent `SecurityManager` API.

Current implementation strengths include SQLCipher, Argon2, Android Keystore wrapping, PIN/biometric unlock paths, and no plaintext database fallback.

Tracked hardening includes biometric enrollment secret lifetime (#3), KDF/secret memory hygiene, and update signer/package trust (#11).

See `docs/security/` and ADR-007.

## 13. AI architecture — central intelligence with governed access

AI is expected to become one of Yonte's most connected and possibly most-used interfaces. Connectivity must be achieved through semantic contracts, not direct database access.

Never design:

```text
AI -> YonteDatabase
AI -> notes/tasks tables
feature ViewModel -> external AI provider directly
```

Preferred model:

```text
User
  |
AI Assistant
  |
Authorization + Confirmation
  |
Knowledge / Action Gateways
  |
Feature-owned contracts
  |
Notes / Tasks / Calendar / Files ...
```

AI may understand semantic types such as NOTE, TASK, EVENT, FILE. It must not know Room entities, DAOs, SQLCipher internals, or feature storage implementation.

AI reads normalized knowledge items only through permissioned gateways and executes writes through explicit commands/contracts with revision/conflict protection.

## 14. AI permissions and confirmations

AI capabilities are controlled by the user from AI settings and are scoped per domain/operation.

Examples:

- Notes: read / create / update / delete.
- Tasks: read / create / update / complete / delete.
- Calendar: read / create / update / delete.
- sensitive/files/financial domains may require just-in-time consent even when a broad feature capability exists.

Authorization answers **"may AI perform this class of operation?"**

Confirmation answers **"must this specific action be approved now?"**

Policy direction:

- ordinary permitted read/search/summarize may proceed.
- explicit user command to create a low-risk item can be sufficient intent if creation capability is enabled.
- modification of existing important data requires appropriate preview/confirmation based on impact.
- bulk modification requires confirmation.
- deletion requires confirmation.
- permanent deletion requires strong confirmation.
- sensitive reads use just-in-time confirmation where required.
- financial/security actions require elevated confirmation.

See `docs/ai/permissions/` and `docs/ai/actions/CONFIRMATION_POLICY.md`, ADR-005.

## 15. AI context, memory, privacy, and derived data

Keep three concepts separate:

- source data — owned by the originating feature.
- AI context — temporary material supplied for the current operation.
- AI memory — intentionally retained AI-specific memory under explicit governance.

Sensitive content is not copied into AI memory automatically.

When external AI providers are used, Yonte must make the processing boundary clear and minimize content. Do not claim external-provider data is globally "deleted from AI memory" unless the provider contract actually proves that. Yonte can guarantee what it stores and reuses locally; provider handling follows the selected provider policy.

Derived data such as summaries/embeddings/recommendations must retain source provenance/revision so it can be invalidated when the source is edited, deleted, or becomes inaccessible.

See `docs/ai/context/`, `providers/`, and `derived/`.

## 16. Backup and disaster recovery

Backup/recovery is a top-tier platform responsibility.

Portable manual backup credentials must have explicit symmetric semantics. Current code has a known mismatch between an "independent backup passphrase" contract and manual export using the active app session key; this is tracked in #12 and must be resolved before claiming a portable recovery model.

Target recovery model may separate:

- a high-entropy Backup Master Key.
- device-local Keystore wrapping for unattended backup.
- a portable recovery capsule protected by a user recovery passphrase.

Restore must be staged and non-destructive to current live data until validation succeeds:

```text
backup file
 -> size/format validation
 -> authentication/decryption
 -> integrity verification
 -> schema/format compatibility
 -> staging parse
 -> record validation
 -> conflict analysis
 -> restore transaction
 -> post-restore verification
 -> commit
```

On failure, live data remains untouched.

Verified backup generations/last-known-good semantics are preferred over trusting the newest file merely because it exists.

Required failure coverage includes corruption, wrong credential, interrupted write/restore, incompatible versions, missing key/Keystore reset, storage unavailable/full, FTS/index corruption, process death, and partial state.

See `docs/backup/` and ADR-006.

## 17. Errors, failure handling, and observability

Failures cross boundaries as typed domain/platform results rather than raw SQL/HTTP/crypto exceptions.

A canonical failure may expose:

- stable error code.
- severity/category.
- retryability.
- safe user action/message.
- correlation ID.
- structured non-sensitive metadata.

Unknown failures must preserve data, stop unsafe continuation, and produce privacy-safe diagnostics. They must never silently report success.

Operational traces/audit may include actor, entity reference, operation, permission used, result, timing, error code, and correlation ID — but not note bodies, passphrases, keys, tokens, or protected content.

See `docs/platform/errors/` and `docs/platform/observability/`.

## 18. Concurrency and consistency

Important mutations define transaction boundaries and conflict behavior explicitly.

Use revision/version checks for optimistic concurrency where stale writes are possible, especially AI/automation flows.

Multi-step operations define atomicity or safe-abort/compensation behavior; do not rely on implicit callback order.

Repeated event/background-job delivery should be idempotent where practical.

See `docs/data/consistency/`.

## 19. Search, indexing, files, background work, and time

Cross-cutting capabilities remain governed without forcing premature frameworks:

- **Search/indexing:** feature content ownership remains local; shared search indexes must not become an unauthorized content store.
- **Attachments/files:** owning domain controls semantics; sensitive files are encrypted and referenced by stable IDs rather than arbitrary filesystem paths.
- **Background work:** WorkManager/OS adapters are thin triggers over high-level use cases; workers do not construct database/security/service graphs.
- **Clock/time:** business rules use an injectable clock where deterministic behavior/testing matters; do not scatter uncontrolled `System.currentTimeMillis()` into long-lived domain policy.
- **Performance/resource budgets:** battery, memory, background work, DB writes, and AI provider/token/cost budgets are considered at feature design time and measured where relevant.

See `docs/platform/` and `docs/data/files/`.

## 20. Future sync readiness

Yonte remains local-first and does not add sync merely to prepare for sync.

However, identity, revisioning, tombstones/lifecycle, provenance, and conflict semantics should avoid choices that make future sync impossible.

If sync is later introduced:

- local database remains the primary user experience source of truth during offline operation.
- sync is an explicit boundary/adapter.
- server schema does not become the domain model by default.
- conflict policy is domain-aware, not blind last-write-wins for all content.

See `docs/data/sync/SYNC_READINESS.md`.

## 21. Release, update trust, and rollout

Release safety distinguishes code rollback from persistent-data rollback. They are not equivalent.

Security/data migrations must document compatibility with older/newer app versions before shipment.

Yonte update hardening must verify package identity and signer trust independently from mutable remote update metadata. Remote SHA remains download-integrity evidence, not the sole signer trust anchor.

Feature flags may stage risky capabilities/UX, but may never disable security/data-integrity invariants. Flags need an owner, default, removal condition, and tests and must not require a remote service for basic local-first usability.

See `docs/platform/release/RELEASE_GOVERNANCE.md` and issue #11.

## 22. Versioning strategy

Do not couple every evolution dimension to the app version.

Maintain conceptually independent versions where applicable:

- App Version.
- Database Schema Version.
- Contract Version.
- Event Version.
- Backup Format Version.
- Security/Crypto Envelope or Policy Version.
- AI Policy/Capability Version.

Contract evolution follows expand -> migrate consumers -> deprecate -> remove where practical.

Crypto evolution uses versioned envelopes/metadata so new writes can use stronger policy while old protected material remains readable during controlled migration.

See `docs/architecture/constitution/EVOLUTION_AND_VERSIONING.md`.

## 23. Testing and quality doctrine

Compilation is evidence of compilation only.

Yonte uses layered verification:

- unit tests for deterministic domain rules/policies.
- mapping/contract tests at boundaries.
- integration tests for repositories/DI/critical adapters.
- UI tests only where behavior cannot be proven at a narrower layer.
- security tests for auth/key/permission/secret lifecycle.
- migration tests for persistent schema evolution.
- backup/recovery corruption and fault-injection tests.
- architecture checks that make forbidden dependency/type leakage fail automatically after migration.
- canonical GitHub Actions CI as repository verification evidence.

Every high-impact confirmed defect receives focused regression coverage when technically practical.

See `docs/platform/testing/QUALITY_GATES.md`.

## 24. Architecture enforcement

Documentation is not enough. After a boundary is migrated, automated checks should make regression difficult to express.

Target guard coverage includes:

- no feature -> feature implementation dependencies.
- no feature -> app dependencies.
- no core -> feature/app dependencies.
- no Room/DAO/persistence entity leakage into feature presentation/domain.
- no feature UI state based on `*Entity` persistence types.
- no direct infrastructure construction in workers/UI where a high-level injected contract is required and reliably checkable.

Important sequencing rule: **migrate first, enforce the new invariant immediately after/with the migration**. Do not intentionally break the baseline with a future-state guard before legitimate current code is migrated.

See issue #6.

## 25. Agent and human governance

Every non-trivial change begins with repository inspection and a change-impact map covering ownership, files/modules, contracts, data/schema, security/privacy, events/AI permissions, backup compatibility, failures, tests, and rollback/safe-abort behavior.

Scope is fenced. Crossing into security/database/backup/migrations/contracts/CI/release/another feature requires explicit justification and the applicable gate.

No silent technical debt. Critical/high debt is not accepted as "later cleanup". Any small accepted debt must be tracked with rationale, impact, owner/removal condition, and target removal point.

Current `AGENTS.md` requires independent implementer/reviewer/verifier roles for non-trivial software work. A future governance improvement (#13) may make the mechanism tool-agnostic while preserving or strengthening independence/evidence requirements; it must not be used as a shortcut to self-approve sensitive changes.

## 26. Definition of architectural progress

A change moves Yonte toward the target only if it produces at least one concrete improvement without unjustified cost, such as:

- lower implementation coupling.
- clearer domain ownership.
- stronger security/privacy boundary.
- safer data migration/recovery.
- improved testability/observability.
- smaller dependency surface.
- better failure isolation.
- more reliable compatibility/versioning.
- reduced future change ripple.

Moving code between folders without improving ownership or change isolation is not architectural progress.

Adding abstractions/modules with no concrete independent reason to change is not architectural progress.

## 27. Execution roadmap — dependency gates

The roadmap is dependency-driven, not a rigid calendar.

### Phase 0 — Baseline health

Goal: trustworthy current baseline before broad refactors.

- keep canonical CI green.
- fix P0 correctness/security defects.
- preserve user data and current behavior.

Current principal blocker: #3 biometric enrollment asynchronous session-key lifetime.

### Phase 1 — Governance and evolution-safety baseline

- maintain ADRs/constitution/change gates/Definition of Done.
- configure committed Room schema v1 baseline.
- establish migration-test support before any DB version increment.
- preserve known backup compatibility fixtures/round-trip evidence.
- enforce only rules legitimate current architecture can satisfy.

Principal tracked prerequisite: #4.

### Phase 2 — Boundary and composition hardening

- introduce Notes-owned domain contract boundary (`:domain:notes` preferred first step).
- map persistence `NoteEntity <-> Note` internally.
- remove Room/DAO/entity knowledge from Notes presentation.
- remove unnecessary dependency surface after verified import analysis.
- make Settings ViewModel lifecycle-owned (#8).
- split Settings orchestration by cohesive responsibility without module explosion (#9).
- thin ScheduledBackupWorker into an OS adapter over a high-level backup job (#10).
- enable semantic architecture guards after each migrated boundary (#6).

### Phase 3 — Backup and recovery hardening

- resolve manual portable-backup credential semantics (#12).
- implement staged restore safety.
- verify backup generations/last-known-good behavior where justified.
- formalize recovery-key lifecycle and backward-compatible backup-format evolution.
- fault-test destructive/recovery paths.

### Phase 4 — Global identity and relationships

- add stable entity references/revisions/provenance/ownership.
- add relationship semantics without centralizing feature content.
- use reviewed Room migrations backed by committed schemas/tests.

### Phase 5 — Authorization and consent

- implement user-owned per-feature AI capabilities.
- separate authorization from action confirmation.
- add resource/risk/sensitivity policy.

### Phase 6 — Commands, queries, and events

- introduce typed integration contracts where cross-feature behavior requires them.
- use events for completed facts/fan-out.
- use transactional outbox/idempotency only where reliability requires it.

### Phase 7 — AI platform integration

- knowledge gateway.
- action gateway.
- capability registry.
- provider boundary/privacy signaling.
- context/memory separation.
- provenance and derived-data invalidation.
- revision/conflict-safe AI writes.
- failure isolation so AI/provider failure never breaks core local features.

### Cross-cutting hardening

Apply when the affected surface changes rather than waiting for a numbered phase:

- update signer/package trust (#11).
- security memory hygiene and crypto agility.
- accessibility/localization/RTL.
- resource/performance budgets.
- release/rollback/feature rollout.
- secrets/config hygiene.
- observability/privacy-safe audit.

See `docs/architecture/migration/MIGRATION_PHASES.md` for the authoritative phase gate wording.

## 28. Current tracked hardening register

Operational issue state is authoritative on GitHub; this list records why each tracked item matters to the master plan.

- #3 — P0 biometric enrollment async secret lifetime.
- #4 — Room schema export/migration-test baseline.
- #6 — semantic architecture guard after boundary migration.
- #7 — Notes domain/persistence decoupling.
- #8 — lifecycle ownership for SettingsViewModel.
- #9 — Settings responsibility decomposition without over-modularization.
- #10 — thin ScheduledBackupWorker / high-level backup job boundary.
- #11 — independent update signer/package trust validation.
- #12 — explicit symmetric portable backup credentials.
- #13 — tool-agnostic agent governance without weakening independent review.

Closed/completed issues remain in GitHub history; do not keep stale "current status" details here when operational state changes frequently.

## 29. Decisions already accepted

The following durable decisions are recorded in ADRs and summarized here:

- ADR-001 — global cross-feature entity identity/reference model.
- ADR-002 — feature-owned data semantics/bounded-context ownership.
- ADR-003 — commands/queries/events instead of implementation coupling.
- ADR-004 — reliable events use idempotency/outbox only where needed.
- ADR-005 — AI uses capability authorization plus risk-based confirmation.
- ADR-006 — portable recovery and unattended device key wrapping are separate concerns.
- ADR-007 — authentication, authorization, and key wrapping are distinct security boundaries.

See `docs/architecture/decisions/README.md`.

## 30. Decisions intentionally not made yet

Avoid prematurely freezing these choices until evidence requires them:

- a `:data:<feature>` Gradle module for every domain.
- server/cloud sync provider or account architecture.
- universal event sourcing.
- one global content table for all features.
- a generic all-powerful security manager.
- a global generic domain module containing every feature model.
- a microservice-style architecture inside the Android app.
- a remote feature-flag dependency for core local behavior.
- a specific external AI provider as permanent architecture.

These remain design options, not commitments.

## 31. How this plan must be maintained

A material product/architecture decision must not exist only in chat, a commit message, or an engineer's memory.

Update process:

1. inspect current code/docs/ADRs first.
2. if the change is a durable architecture decision, create or supersede an ADR.
3. update this Master Plan when product destination, accepted architecture, dependency order, or major tracked work changes.
4. update the focused authoritative doctrine/specification for implementation-level rules.
5. keep operational status in Issues/PRs/CI rather than embedding volatile SHAs/runs here.
6. remove or clearly mark obsolete documents so there is one discoverable path to current truth.

## 32. Reviewer starting path

A new engineer should normally read in this order:

1. `docs/YONTE_MASTER_PLAN.md` — destination and execution map.
2. `docs/architecture/YONTE_CONSTITUTION.md` — non-negotiable architectural laws.
3. `docs/architecture/decisions/README.md` — durable decisions and trade-offs.
4. `docs/architecture/migration/MIGRATION_PHASES.md` — dependency gates.
5. specialized area docs (`docs/security/`, `docs/backup/`, `docs/data/`, `docs/ai/`, `docs/integration/`, `docs/platform/`, `docs/design/`).
6. current GitHub Issues/PR and actual code for live implementation state.

## 33. Final vision test

Before approving a major change, ask:

> Does this make Yonte more capable while keeping features independently evolvable, user data safer, security explicit, recovery trustworthy, and future AI deeply connected without gaining uncontrolled authority?

If the answer is no, the change should be redesigned even if it is faster to implement.

The target is not the maximum number of modules, documents, managers, or abstractions.

The target is a platform that is **secure, cohesive, highly connected, low-coupled, reviewable, recoverable, and able to evolve for years without architecture collapse**.
