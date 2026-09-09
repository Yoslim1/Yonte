# Yonte Master Vision & Execution Plan

## 1. Purpose

This is Yonte's durable project map.

It records the final direction we are building toward, the accepted architecture decisions, the dependency order for implementation, and the authoritative documents that contain deeper detail.

It is intentionally broader than focused ADR/security/data documents, but it must not duplicate their implementation detail. When a durable decision changes, update the relevant ADR first and then synchronize this file.

## 2. Authority order

When artifacts disagree, use this order:

1. explicit current user requirement.
2. security/data-integrity invariants and `AGENTS.md` execution policy.
3. accepted Architecture Decision Records.
4. Architecture Constitution and specialized security/data/backup doctrine.
5. actual production code/build/CI for current implementation facts.
6. this Master Plan for destination and dependency order.
7. Issues/PRs for live execution status.

Chat is a design workspace, not the durable source of truth. Material final decisions must enter the repository.

## 3. Product destination

Yonte evolves into a **Secure Personal Intelligence Workspace**: a local-first personal platform in which multiple domains cooperate without sacrificing user ownership, security, recoverability, or independent evolution.

Long-term capability families may include:

- Notes and personal knowledge.
- Tasks and actionable work.
- Calendar/events and time-oriented context.
- Files/attachments and personal artifacts.
- Habits or other personal workflows when justified by product value.
- Financial/personal sensitive domains only behind stronger privacy/authorization gates.
- AI Assistant as a first-class governed interface over user-authorized capabilities.
- automation and cross-feature workflows built through stable contracts.

No new product domain is added merely because it appears here. Foundation readiness and product value determine what ships.

Current product identity direction:

- positioning: **Secure Personal Intelligence Workspace**.
- Deep Navy `#071A33`, Cyan `#00C8FF`, Silver `#DCE7F5`.
- layered stylized `Y` visual identity.
- English concept remains the current preferred visual reference; Arabic/RTL is first-class and must be deliberately designed.

## 4. Architectural model

Yonte is a **modular monolith / personal platform**, not microservices inside Android.

Mental model:

```text
                         YONTE PLATFORM

       security policy / data identity / recovery / quality laws
                              |
        -------------------------------------------------
        |              |              |                |
      Notes          Tasks         Calendar           Files ...
        |              |              |                |
        ------------- stable contracts -----------------
                              |
                commands / queries / events
                              |
                   governed AI / automation
```

Features are sovereign bounded contexts ("countries/islands") under shared platform laws.

A feature owns:

- its domain meaning and rules.
- its presentation/UX.
- its data semantics.
- its contracts/capabilities.
- its internal implementation choices.

A feature may substantially change internally without forcing unrelated features to change.

**High connectivity is expected. High implementation coupling is not.**

## 5. Platform/core responsibilities

Shared platform capabilities exist only when they are genuinely cross-cutting.

Current/future platform areas include:

- encrypted physical storage infrastructure.
- security/authentication/authorization policy.
- key lifecycle/crypto infrastructure.
- backup/recovery infrastructure.
- update trust/release safety.
- design system/accessibility/localization laws.
- navigation/composition infrastructure.
- errors/observability/background-work/time policies.

The `app` module remains the composition root. It wires implementations to contracts and owns top-level Android lifecycle/navigation orchestration, not feature business logic.

Avoid an omnipotent `SecurityManager`, `CoreManager`, service locator, or generic platform module that becomes a dumping ground.

## 6. Module strategy

Modules express real ownership/change isolation, not architectural decoration.

Rules:

- feature implementation modules do not depend on other feature implementation modules.
- `core` does not depend on feature/app.
- domain contract modules are introduced only when they create a useful stable boundary.
- no generic `core:domain` containing every feature model.
- do not create `:data:<feature>` modules merely to match textbook layering.
- extract a data module only when it can own persistence/transaction concerns cleanly and reduce ripple.

Preferred first Notes migration (ADR-008):

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

`:domain:notes` may cohesively own `Note`, `NotesRepository`, and narrower ports such as a backup-oriented snapshot/read contract where justified.

Persistence implementation can remain in `core:database` initially; a future `:data:notes` extraction is evidence-driven, not precommitted.

## 7. Documentation/file organization

Optimize for **cohesion + reviewability**.

A file may contain two or three small responsibilities when they share owner, reviewer, authority, lifecycle, and normal reason to change.

Split when concerns develop independent ownership/review/security risk or become difficult to understand safely.

Avoid both megafiles and fragmented micro-documents.

The Master Plan is a deliberate broader orientation document; focused rules remain in specialized docs/ADRs.

## 8. Data ownership and physical storage

One encrypted Room database may physically store multiple bounded contexts. Shared physical storage does not grant shared ownership.

Rules:

- each domain owns schema semantics and repository contracts.
- Room entities/DAOs/raw SQL/SQLCipher details are persistence implementation.
- feature presentation/domain does not expose persistence entities.
- no feature reads another feature's tables directly.
- cross-feature references use stable identity/contracts.
- migrations preserve user data; no destructive migration shortcut.
- committed Room schemas + migration-test evidence are required before schema evolution.

Current confirmed debt: Notes presentation still depends on `NoteEntity`; migrate it before enabling the semantic persistence guard (#7 then #6).

## 9. Global entity identity — data passports

Global entity identity belongs to the **data/platform identity boundary**, not Security (ADR-001, ADR-010).

A durable cross-feature reference may contain, where needed:

- globally stable entity ID.
- semantic entity type.
- owning/source domain.
- owner/workspace/user context.
- revision/version.
- provenance/source reference.
- lifecycle/sensitivity metadata.

The registry/relationship layer stores identity and relationship metadata, not duplicate protected feature content.

Cross-feature relationships use stable references instead of persistence classes.

Revision/version supports optimistic concurrency so stale AI/automation writes cannot silently overwrite newer user changes.

## 10. Integration model

Three integration forms remain distinct:

- **Command** — explicit request to perform an action.
- **Query** — explicit request for information.
- **Event** — fact emitted after a state change completed.

Commands/queries handle synchronous request-response behavior. Events handle fan-out/decoupled reactions.

Do not turn an Event Bus into the universal call mechanism.

Events carry references and non-sensitive metadata by default: **references, not secrets**.

Transactional outbox/idempotency is introduced only when atomic reliable delivery is genuinely required.

## 11. Security doctrine

Security is a platform authority, not a feature utility bag.

Stable invariants:

- user owns their data.
- local-first/private-by-default.
- deny by default for sensitive capabilities.
- least privilege / least capability.
- no plaintext sensitive data at rest.
- feature code never receives unconstrained master-key authority.
- authentication, authorization, encryption/key wrapping, and action confirmation remain separate concepts.
- security failure is fail-closed where appropriate.
- crypto implementations/parameters may evolve only through versioned compatibility-aware migrations.

Security may own narrow services for **actor/session identity**, authentication, authorization policy, key vault/crypto, privacy policy, audit decision metadata, and update-trust verification.

Security does **not** own global entity identity or every feature's capability vocabulary (ADR-010).

Each bounded context owns semantic capability/resource definitions; Security evaluates policy over stable identifiers/references.

Current strengths include SQLCipher, Argon2, Android Keystore wrapping, PIN/biometric unlock paths, and no plaintext DB fallback.

Current hardening priorities include:

- biometric async secret lifetime (#3).
- candidate-key derivation vs authenticated-session commit (#15).
- KDF secret-memory hygiene with compatibility proof (#16).
- automatic-backup authority lifecycle (#14).
- update signer/package trust (#11).

## 12. Authentication and key lifecycle

A candidate secret/key is not authenticated session state merely because it can be derived or unwrapped.

Preferred passphrase flow:

```text
derive candidate
 -> validate protected database/session
 -> commit wrapped session key
 -> publish authenticated/unlocked state
```

Purpose-specific key authority stays explicit:

- active session cache.
- PIN convenience cache.
- biometric-gated cache.
- automatic-backup background authority.
- future portable recovery material.

Do not collapse these into one interchangeable generic key cache.

Temporary mutable key material is zeroed when ownership permits. Avoid creating extra secret copies without a demonstrated lifecycle need.

## 13. Authorization and consent

Authorization answers:

`May actor A perform capability C on resource R now?`

Security owns:

- decision model (`ALLOW`, `DENY`, `CONFIRMATION_REQUIRED`).
- policy/risk/sensitivity evaluation.
- confirmation rules.
- policy versioning.

Bounded contexts own:

- semantic capability vocabulary (for example Notes read/create/update/delete).
- resource/business semantics.

Global entity identity stays in the data/platform identity boundary.

This separation is durable in ADR-010.

## 14. AI architecture

AI is intended to become deeply connected, but connectivity is through governed semantic contracts—not direct storage access.

Never:

```text
AI -> YonteDatabase
AI -> notes/tasks tables
Feature ViewModel -> external AI provider directly
```

Preferred:

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

AI may understand NOTE/TASK/EVENT/FILE semantics, but not Room/DAO/SQLCipher implementation.

Writes use explicit commands and revision/conflict checks.

## 15. AI permission, context, memory, and privacy

AI permissions are user-controlled per domain/operation.

Authorization and confirmation are distinct:

- ordinary permitted read/search/summarize may proceed.
- explicit low-risk create may use the user's current command as sufficient intent when standing permission allows it.
- meaningful modification may require preview/confirmation based on impact.
- bulk modification requires confirmation.
- delete requires confirmation.
- permanent delete requires strong confirmation.
- sensitive read/external disclosure may require just-in-time consent.
- financial/security/recovery actions require elevated confirmation.

Keep separate:

- **source data** — owned by originating domain.
- **AI context** — temporary material for current operation.
- **AI memory** — deliberately persisted AI-specific memory under explicit governance.

Sensitive source content does not automatically become AI memory.

External provider handling must be described honestly; Yonte only guarantees local retention/reuse behavior it actually controls.

Derived summaries/embeddings/recommendations keep source provenance/revision so they can be invalidated.

## 16. Backup and disaster recovery

Backup/recovery is top-tier platform infrastructure.

Manual portable backup and scheduled unattended backup share security/recovery laws but have different credential lifecycles.

Current known issues:

- manual export contract says independent backup passphrase while current export uses active session-key lineage (#12).
- automatic-backup key presence is not perfectly aligned with enabled/configured state (#14).
- Worker currently constructs too much infrastructure and must become a thin OS adapter after policy/data dependencies are explicit (#10).
- current restore does not yet enforce the full version-aware staged/conflict-safe pipeline required by policy (#18).

Portable recovery target may eventually separate:

- high-entropy Backup Master Key.
- device-local Keystore wrapping for unattended backup.
- portable recovery capsule protected by user recovery passphrase.

Restore is staged and non-destructive:

```text
backup
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

Failure leaves live data untouched.

Support verified generations/last-known-good semantics when justified; do not trust "latest" merely because it exists.

## 17. Errors and observability

Failures cross boundaries as typed safe results rather than raw SQL/HTTP/crypto exceptions.

Canonical failure metadata may include:

- stable code/category.
- severity.
- retryability.
- safe user action/message.
- correlation ID.
- non-sensitive structured metadata.

Unknown failures preserve data and stop unsafe continuation.

Audit/observability may record actor, capability, entity reference, result, timing, error code, policy version, and correlation ID—but never bodies, passphrases, keys, tokens, or protected content.

## 18. Concurrency and consistency

Important mutations define transaction/conflict behavior explicitly.

Use revision/version checks where stale writes are possible, especially AI/automation.

Multi-step operations define atomicity or safe-abort/compensation; do not rely on callback ordering.

Repeated event/background-job delivery is idempotent where practical.

## 19. Search, files, background work, time, resources

Cross-cutting laws:

- **Search/indexing:** feature content ownership remains local; a shared index cannot become an unauthorized content store.
- **Attachments/files:** owning domain controls semantics; protected files are encrypted and referenced by stable IDs.
- **Background work:** WorkManager/OS callbacks are thin adapters over high-level jobs, not service/database/security composition roots.
- **Clock/time:** injectable clocks are used where deterministic business/security policy matters; wall clock is not scattered through long-lived policy.
- **Resources:** battery, memory, background work, DB writes, and future AI token/provider cost have explicit budgets when relevant.

## 20. Future sync readiness

Yonte stays local-first and does not add sync merely to prepare for sync.

Identity, revisions, tombstones/lifecycle, provenance, and conflict semantics should avoid choices that make future sync impossible.

If sync is later justified:

- local DB remains the offline user-experience source of truth.
- sync is an explicit adapter/boundary.
- server schema does not become the domain model by default.
- conflict policy is domain-aware, not universal blind last-write-wins.

## 21. Release/update/rollout

Code rollback and persistent-data rollback are different problems.

Security/data migrations document compatibility before shipment.

Update trust uses installed package identity/signing lineage as the local trust anchor; remote SHA is integrity evidence, not signer authority (ADR-009).

Feature flags may stage risky optional behavior but may never disable security/data-integrity invariants. Core local usability must not depend on a remote flag service.

## 22. Versioning

Keep evolution dimensions conceptually independent where applicable:

- App Version.
- DB Schema Version.
- Contract Version.
- Event Version.
- Backup Format Version.
- Security/Crypto Envelope or Policy Version.
- AI Policy/Capability Version.

Contracts evolve through expand -> migrate -> deprecate -> remove when practical.

Crypto evolution uses versioned envelopes/metadata with controlled compatibility migration.

## 23. Testing and Definition of Done

Compilation proves only compilation.

Required evidence is layered:

- unit tests for deterministic rules/policies.
- mapping/contract tests at boundaries.
- integration tests for repositories/DI/critical adapters.
- UI tests only where narrower layers cannot prove behavior.
- security tests for authentication/key/permission/secret lifecycle.
- Room migration tests for schema evolution.
- backup/recovery corruption/fault tests.
- semantic architecture guards after migration.
- canonical GitHub Actions CI.

A high-impact defect gets focused regression coverage when technically practical.

No Critical/High technical debt is accepted as silent "later cleanup". Any small accepted debt needs rationale, impact, removal condition, and tracked target.

## 24. Architecture enforcement

After a boundary is migrated, automated checks should make regression difficult to express.

Target enforcement includes:

- no feature -> feature implementation dependencies.
- no feature -> app dependency.
- no core -> feature/app dependency.
- no Room/DAO/persistence entity leakage into feature presentation/domain.
- no UI state based on persistence `*Entity` types.
- no Worker/UI infrastructure graph construction where a high-level contract is required and reliably checkable.

**Migrate first, enforce immediately after/with the migration.** Do not intentionally make CI red with a future-state guard before legitimate current code is migrated.

## 25. Agent/human governance

Every non-trivial change begins with inspection and a change-impact map covering:

- ownership/modules/files.
- contracts/dependencies.
- data/schema/migration.
- security/privacy/secrets.
- events/AI permissions.
- backup compatibility.
- failures/tests/rollback or safe-abort.

Scope crossing security/database/backup/migrations/contracts/CI/release/another domain requires explicit justification and the applicable gate.

Current `AGENTS.md` requires independent implementer/reviewer/verifier roles for non-trivial software work. Future tool-agnostic governance (#13) may change the mechanism but must preserve independence/evidence and must never become a self-approval shortcut.

## 26. Execution roadmap — dependency gates

The roadmap is dependency-driven, not calendar-driven. Deferred items are re-evaluated whenever an adjacent boundary changes.

### Phase 0 — Baseline health

- keep canonical CI green.
- fix P0 biometric asynchronous secret lifetime (#3).
- preserve user data/current behavior.

**Gate:** no broad runtime migration while a relevant P0 correctness/security defect or unexplained CI failure remains open.

### Phase 1 — Evolution-safety baseline

- maintain ADRs/constitution/change gates/Definition of Done.
- commit Room schema v1 and migration-test support before any DB version increment (#4).
- preserve backup compatibility fixtures/round-trip evidence.
- enforce only rules the legitimate current architecture can satisfy.

### Phase 2 — Boundary, session, and composition hardening

Execute by dependency, not merely issue number:

1. Notes domain/persistence boundary (#7 / ADR-008).
2. candidate-key derivation vs authenticated-session commit (#15).
3. Settings long-running operation owners (#9).
4. Settings presentation lifecycle ownership after operation lifetimes are explicit (#8).
5. automatic-backup key lifecycle correctness (#14).
6. thin ScheduledBackupWorker after policy/data/key dependencies are explicit (#10).
7. semantic architecture enforcement after each migrated boundary (#6).

Independent safe work may move within this phase when its prerequisite is already satisfied.

### Phase 3 — Backup/recovery hardening

- explicit symmetric manual portable-backup credentials (#12).
- version-aware staged restore with semantic validation, explicit conflict planning, atomic apply, and post-restore verification (#18).
- verified generations/last-known-good where justified.
- recovery-key lifecycle + versioned backward compatibility.
- corruption/interruption/wrong-credential/incompatible-version/storage failure tests.

### Phase 4 — Global identity/relationships

- stable entity references/revisions/provenance/ownership.
- relation semantics without centralizing feature content.
- reviewed Room migrations using committed schemas/tests.

### Phase 5 — Authorization/consent

- capability/resource/risk/sensitivity policy.
- bounded contexts define semantic capability vocabulary; Security evaluates it (ADR-010).
- user-owned AI permission persistence.
- authorization remains separate from confirmation.

### Phase 6 — Commands/queries/events

- typed request/response contracts where integration requires them.
- events for completed facts/fan-out.
- transactional outbox/idempotency only where reliability requires it.

### Phase 7 — AI platform

- knowledge/action gateways.
- capability registry/discovery.
- provider/privacy boundary.
- context vs memory governance.
- provenance/derived-data invalidation.
- revision-safe writes.
- provider failure isolation from core local functionality.

### Cross-cutting foundation work

Apply when safe and relevant rather than postponing by phase number:

- installed-package/signing-lineage update trust (#11 / ADR-009).
- KDF memory hygiene only after byte-for-byte compatibility vectors (#16).
- PIN rate-limit clock hardening only after its clock/reboot threat model is explicit.
- accessibility/localization/RTL.
- resource/performance budgets.
- release/rollback/rollout.
- secrets/config hygiene.
- privacy-safe observability/audit.

Authoritative detailed gate wording: `docs/architecture/migration/MIGRATION_PHASES.md`.

### Foundation completion gate

Issue #17 is the live completion gate for the current project direction. No new product domain/service starts until the shared platform foundation is reviewably ready, unless the user explicitly changes that gate.

A green build alone does not satisfy foundation completion. Remaining items must either be implemented and verified or explicitly reviewed and accepted as not required for the foundation with rationale recorded in the repository.

## 27. Current hardening register

Live issue state on GitHub is authoritative. Current tracked foundation work includes:

- #3 — P0 biometric async secret lifetime.
- #4 — Room schema export/migration-test baseline.
- #6 — semantic architecture guard after boundary migration.
- #7 — Notes domain/persistence decoupling.
- #8 — Settings presentation lifecycle ownership.
- #9 — Settings operation/responsibility decomposition.
- #10 — thin ScheduledBackupWorker/high-level backup job.
- #11 — independent update signer/package trust.
- #12 — symmetric portable backup credentials.
- #13 — tool-agnostic independent agent review governance.
- #14 — automatic-backup key cache must match enabled/configured state.
- #15 — separate candidate-key derivation from authenticated session commit.
- #16 — remove immutable KDF secret copies with UTF-8 compatibility proof.
- #17 — live foundation completion gate before any new product domain/service.
- #18 — version-aware staged/conflict-safe restore with post-restore verification.

## 28. Accepted durable decisions

- ADR-001 — global cross-feature entity identity/reference model.
- ADR-002 — bounded-context data ownership.
- ADR-003 — commands/queries/events instead of implementation coupling.
- ADR-004 — idempotency/outbox only where reliable event delivery requires it.
- ADR-005 — capability-based AI authorization plus risk-based confirmation.
- ADR-006 — unattended device wrapping and portable recovery are separate concerns.
- ADR-007 — authentication, authorization, and key wrapping remain distinct.
- ADR-008 — introduce `:domain:notes` first; defer `:data:notes` until it gives real isolation.
- ADR-009 — installed package/signing lineage is the update trust anchor.
- ADR-010 — Security owns authorization policy; data/platform owns global entity identity and bounded contexts own capability semantics.

Index: `docs/architecture/decisions/README.md`.

## 29. Intentionally deferred decisions

These remain options until evidence changes the decision:

- `:data:<feature>` for every domain.
- server/cloud sync provider/account architecture.
- universal event sourcing.
- one global content table.
- generic all-powerful security manager.
- global generic domain module containing every feature model.
- microservice-style architecture inside Android.
- remote feature-flag dependency for core local behavior.
- permanent commitment to one external AI provider.

### Deferral review rule

A deferral is **not permanent**.

At every related task ask:

1. Has a new dependency made the deferred capability necessary now?
2. Would implementing it now reduce total coupling/rework/risk?
3. Can it be introduced safely with current tests/migration/recovery evidence?
4. Does continuing to defer create hidden debt or force a knowingly wrong boundary?

If the answer shows the deferral is now harmful, promote the work. If implementing it would still be speculative architecture, keep it deferred and record why.

## 30. Maintenance rule

A material final decision must not live only in chat, memory, or a commit message.

Process:

1. inspect current code/docs/ADRs.
2. create/supersede an ADR for durable architecture decisions.
3. update this Master Plan when destination, accepted architecture, dependency order, or major tracked foundation work changes.
4. update focused doctrine/specs for detailed rules.
5. keep volatile operational status in Issues/PRs/CI.
6. remove/redirect obsolete documents so there is one discoverable path to truth.

## 31. Reviewer starting path

1. `docs/YONTE_MASTER_PLAN.md`
2. `docs/architecture/YONTE_CONSTITUTION.md`
3. `docs/architecture/decisions/README.md`
4. `docs/architecture/migration/MIGRATION_PHASES.md`
5. specialized `docs/security/`, `docs/backup/`, `docs/data/`, `docs/ai/`, `docs/integration/`, `docs/platform/`, `docs/design/`.
6. current Issues/PRs + actual code for live state.

## 32. Final vision test

Before approving a major change ask:

> Does this move Yonte toward a secure, recoverable, highly connected platform while keeping bounded contexts independently evolvable and preventing AI/platform infrastructure from gaining uncontrolled authority?

If not, redesign it even if the alternative is faster.

The target is not maximum modules, files, managers, or abstractions.

The target is a platform that is **secure, cohesive, highly connected, low-coupled, reviewable, recoverable, productive to extend, and able to evolve for years without architecture collapse**.
