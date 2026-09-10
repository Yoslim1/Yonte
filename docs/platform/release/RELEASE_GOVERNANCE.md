# Release Governance

## Purpose

Define safe shipment, staged exposure, and rollback behavior when application code, persistent data, security policy, and update trust evolve at different speeds.

## Release safety

- Persistent schema migrations are forward evolution; destructive rollback is not an acceptable recovery strategy for user-owned data.
- Before a schema/security/backup format change, define which older and newer app versions can safely open the resulting data.
- A release that can make older binaries incompatible must document that rollback limitation before shipment.
- Update signer/package identity is verified independently from mutable release metadata.
- Release gates include canonical CI, migration/recovery evidence when applicable, and integrity/signature verification.
- Failed or partial upgrades preserve data and expose a safe blocking/recovery state rather than guessing or wiping.
- Code rollback and data-format rollback are different operations and must never be assumed equivalent.

## Staged rollout

- Feature flags control exposure, not correctness or security invariants.
- Security, migration, encryption, and data-integrity requirements stay enforced whether a feature is visible or hidden.
- A flag has an owner, default state, scope, removal condition, and test plan.
- New persistent data remains readable and safe if the visible feature is later disabled.
- Rollout state must not create incompatible schemas or competing business truths without an explicit versioned migration strategy.
- Local-first behavior must not depend on a remote flag service being reachable.
- Use staged rollout for capabilities with meaningful operational/product uncertainty such as AI/provider experiments, large UX transitions, or controlled migrations—not every routine feature.

## Cleanup and rollback

- Remove rollout flags after rollout/rollback uncertainty ends; stale flags are tracked technical debt.
- Do not add downgrade migrations merely to promise universal rollback.
- When safe reverse transformation cannot be guaranteed, preserve data and deliver a compatible forward fix.

## Review questions

Before shipment, reviewers should be able to answer:

1. What can be safely rolled back: code, configuration, visibility, schema, or data format?
2. What remains safe if rollout is disabled after users have created data?
3. Which compatibility/recovery evidence proves that claim?
