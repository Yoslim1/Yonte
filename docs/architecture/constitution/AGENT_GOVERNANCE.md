# Agent Architecture Governance

## Purpose

Define additional architecture expectations for coding agents. `AGENTS.md` remains the execution-policy authority.

## Before non-trivial change

Produce an impact map covering:

- owning feature/platform area.
- expected files/modules.
- public contracts.
- data/schema/migration impact.
- security/privacy impact.
- event/AI permission impact.
- backup/recovery compatibility.
- failure scenarios.
- tests and verification.
- rollback or safe abort path.

## Scope fence

Declare expected allowed areas. Crossing into security, database, backup, migrations, contracts, CI/release, or another feature requires explicit justification and the applicable review gate.

## Review principle

Implementation self-review is insufficient for non-trivial changes. Automated checks and independent review should challenge correctness, security, architecture, data loss, concurrency, lifecycle, performance, tests, and unnecessary abstractions.

## Debt rule

No silent TODO debt. Accepted debt must have rationale, impact, owner/removal condition, and tracking.

## Failure rule

Agents should model reasonable failure classes and safe unknown-failure behavior; they must not add defensive complexity attempting to enumerate every theoretically possible failure.
