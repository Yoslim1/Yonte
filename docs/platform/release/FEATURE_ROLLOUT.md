# Feature Rollout Policy

## Purpose

Allow risky or large capabilities to ship progressively without leaving permanent branching complexity in the product.

## Rules

- Feature flags protect rollout/exposure, not correctness or security invariants.
- Security, migration, encryption, and data-integrity requirements remain enforced whether a feature is visible or hidden.
- A flag has an owner, default state, scope, removal condition, and test plan.
- New persistent data must remain readable/safe if the UI feature is later disabled.
- Rollout state must not create two incompatible schemas or business truths without an explicit versioned migration strategy.
- Flags are removed after rollout/rollback uncertainty ends; stale flags are tracked technical debt.
- Local-first behavior does not depend on a remote flag service being reachable.

## Use

Prefer flags for staged AI/provider experiments, large UX transitions, or migrations that need controlled exposure—not for every routine feature.
