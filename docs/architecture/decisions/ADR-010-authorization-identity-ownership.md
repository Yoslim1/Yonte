# ADR-010: Authorization and Identity Ownership

## Status

Accepted target architecture.

## Context

Yonte needs a shared authorization system that can evaluate users, AI, workers, restore operations, and future actors without turning `core:security` into the owner of every domain concept.

Global entity identity is already a platform/data concern, while each feature/domain owns its business semantics. Hard-coding domain capabilities such as Notes operations inside Security would reverse that ownership and make Security depend conceptually on every future feature.

## Decision

1. Global entity identity/reference semantics belong to the data/platform identity boundary, not to `core:security`.
2. Each bounded context owns its capability vocabulary and resource semantics.
3. Security owns the generic authorization decision model, policy evaluation, confirmation rules, risk handling, and policy versioning.
4. Authorization consumes stable actor/resource references plus capability identifiers; it does not consume DAOs, Room entities, or feature persistence models.
5. A shared capability registry may aggregate capability descriptors for discovery (including future AI tooling) without becoming the semantic owner of those capabilities.
6. Do not introduce feature-specific sealed capability objects such as `ReadNotes` directly inside `core:security`.

## Consequences

- Adding Tasks, Calendar, Files, or other domains does not require editing Security merely to define their business operations.
- Security remains stable while feature vocabularies evolve through versioned contracts.
- AI can discover and request capabilities through governed descriptors without receiving direct feature implementation access.
- Cross-feature authorization uses the same global entity references defined by the data identity boundary.

## Rejected

- A single Security-owned enum/sealed hierarchy containing every feature capability.
- Making Security the owner of global entity identity.
- Feature-specific authorization managers that bypass the shared decision policy.
