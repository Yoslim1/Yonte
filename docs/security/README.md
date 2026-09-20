# Security Documentation Map

## Purpose

Navigate Yonte security doctrine without requiring a reviewer to read the entire security system.

## Authoritative areas

- `SECURITY_DOCTRINE.md` — doctrine index.
- `SECURITY_ARCHITECTURE_AUDIT.md` — current-audit index.
- `THREAT_MODEL.md` — threat-model index.
- `principles/` — core invariants and data classification.
- `architecture/` — authentication, authorization, key lifecycle, and boundary map.
- `threats/` — focused threat domains.
- `reviews/` — current findings and review procedure.

Durable security architecture decisions live with the platform ADRs under `../architecture/decisions/` so one decision system governs the repository.

## Documentation rules

- Group tightly related small security concerns when they share the same boundary, reviewer, authority, and reason to change.
- Split when a concern gains an independent lifecycle, owner, review gate, or enough complexity that the combined document becomes harder to reason about.
- Keep current implementation facts, target policy, threats, decisions, and findings distinguishable; combine them only when doing so cannot blur authority or shipped-vs-target behavior.
- Index files navigate; they may orient the reader briefly but do not duplicate authoritative policy.
- Create a new focused document only when it improves ownership, reviewability, or change isolation—not merely to enforce one topic per file.
- Avoid both security megadocuments and fragmented micro-documents.
