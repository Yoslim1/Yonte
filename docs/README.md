# Yonte Documentation Index

## Start here

1. [Project Status](PROJECT_STATUS.md) — dated, evidence-backed snapshot of branch, Issue, Pull Request, and CI state.
2. [Yonte Master Vision & Execution Plan](YONTE_MASTER_PLAN.md) — durable product destination and dependency order.
3. [Yonte Architecture Constitution](architecture/YONTE_CONSTITUTION.md) — non-negotiable architectural laws.
4. [Architecture Migration Phases](architecture/migration/MIGRATION_PHASES.md) — prerequisite order for implementation.

## Current implementation and risk registers

- [Architecture Foundation Status](architecture/FOUNDATION_STATUS.md) — boundary between target architecture and present implementation.
- [Current Security Findings](security/reviews/CURRENT_FINDINGS.md) — active security debt and resolved baseline items.
- [Current Backup Findings](backup/reviews/CURRENT_FINDINGS.md) — active backup and recovery debt.
- [Data documentation](data/README.md), [AI governance](ai/README.md), [design doctrine](design/README.md), and [integration contracts](integration/README.md) — specialized policy and target boundaries.

## Authority and lifecycle

- GitHub branches, Issues, Pull Requests, and GitHub Actions are the live execution authority.
- AGENTS.md, the Constitution, and accepted ADRs govern repository changes and durable engineering decisions.
- The Master Plan defines the destination; it does not claim future design as shipped behavior.
- Current-finding registers track active debt. Update them when implementation or independently verified evidence changes.
- Historical audits preserve evidence and context but never override current code, CI, or accepted decisions.

## Historical audits

- [2026-09-02 hardening audit](history/audits/2026-09-02-hardening-audit.md)
- [2026-09-10 security lifecycle audit](history/audits/2026-09-10-security-lifecycle-audit.md)

Historical reports are retained because their findings may explain later decisions. Their status claims are not live; use Project Status and GitHub evidence instead.

## Removed obsolete reports

Superseded one-off reports and baseline snapshots were removed in the documentation-current-state cleanup. Their relevant active findings are represented in the current registers above, and Git history retains their prior contents.
