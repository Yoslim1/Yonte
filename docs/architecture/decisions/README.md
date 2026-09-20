# Architecture Decision Records

## Purpose

Index for durable architecture decisions. Each ADR owns one decision and records its consequences.

## Decisions

- `../ADR-001-global-entity-identity.md` — stable cross-feature entity identity.
- `ADR-002-feature-data-ownership.md` — feature-owned domain/data semantics.
- `ADR-003-integration-contracts.md` — commands, queries, and events instead of implementation coupling.
- `ADR-004-event-reliability.md` — idempotency and transactional outbox only where required.
- `ADR-005-ai-authorization.md` — capability-based AI permissions plus risk-based confirmation.
- `ADR-006-backup-recovery-keys.md` — separate unattended device wrapping from portable recovery.
- `ADR-007-security-boundary-separation.md` — authentication, authorization, and key wrapping are distinct.
- `ADR-008-notes-domain-boundary.md` — introduce `:domain:notes` first; defer `:data:notes` until it provides real isolation.
- `ADR-009-update-trust-anchor.md` — trust downloaded updates against installed package identity/signing lineage, not mutable remote signer metadata.
- `ADR-010-authorization-identity-ownership.md` — Security owns authorization policy; data/platform owns global identity and bounded contexts own capability semantics.

## Rule

An ADR changes only through a superseding ADR; implementation notes do not silently redefine a decision.
