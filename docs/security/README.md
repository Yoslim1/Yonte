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

- One responsibility per document.
- Current implementation, target policy, threats, decisions, and findings remain distinct.
- Index files navigate; they do not duplicate policy.
- Create a new focused document only when a genuinely new responsibility exists.
