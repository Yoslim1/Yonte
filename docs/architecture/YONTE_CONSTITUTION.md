# Yonte Architecture Constitution

## Purpose

Entry point for the non-negotiable rules governing Yonte evolution.

## Orientation

For the complete product destination, execution order, major architectural decisions, and long-term roadmap, start with `../YONTE_MASTER_PLAN.md`.

This Constitution remains the authority for architectural invariants. The Master Plan explains where Yonte is going and how the dependency-gated migration is expected to reach it.

## Constitution chapters

- `constitution/CORE_PRINCIPLES.md` — platform-wide engineering axioms.
- `constitution/FEATURE_SOVEREIGNTY.md` — ownership and independence of feature islands.
- `constitution/PLATFORM_BOUNDARIES.md` — contracts, data, security, composition, and communication boundaries.
- `constitution/EVOLUTION_AND_VERSIONING.md` — compatibility and controlled evolution.
- `constitution/AGENT_GOVERNANCE.md` — rules for coding agents and reviews.

Specialized doctrine lives in dedicated domains such as `docs/security/` and `docs/backup/`.

## Authority

Detailed implementation may evolve, but changes that violate a constitutional invariant require an explicit architecture decision and review rather than an incidental refactor.
