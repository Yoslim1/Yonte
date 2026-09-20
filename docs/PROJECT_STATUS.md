# Yonte Project Status

> Snapshot date: 2026-09-20. This file records verified repository state at that date; GitHub Issues, Pull Requests, branches, and GitHub Actions remain the live authority after the snapshot.

## Scope and authority

Use this document to understand what has actually landed, what is pending, and what may begin next. It does not replace the durable product direction in [Yonte Master Vision & Execution Plan](YONTE_MASTER_PLAN.md), architectural laws in [Yonte Architecture Constitution](architecture/YONTE_CONSTITUTION.md), or the dependency order in [Architecture Migration Phases](architecture/migration/MIGRATION_PHASES.md).

When sources disagree, use this order:

1. Current GitHub branch, Issue, Pull Request, and GitHub Actions evidence.
2. AGENTS.md and security/data-integrity invariants.
3. Accepted ADRs and the Architecture Constitution.
4. This dated project-state snapshot.
5. Historical audit records.

## Branch reality

| Branch | Current role at this snapshot | Commit |
| --- | --- | --- |
| main | Existing product baseline. It does not contain the architecture-foundation work described below. | [8517f1c](https://github.com/Yoslim1/Yonte/commit/8517f1c59aa6768699fea5c3467bf99f8aba6aec) |
| architecture-foundation | Integration baseline for foundation work. It contains the closed Issue #3 fix. | [6d8a591](https://github.com/Yoslim1/Yonte/commit/6d8a59133ea5d5388408cfdc9de54c19e938eb7b) |

Foundation work targets architecture-foundation through focused pull requests. No work is merged directly into main by this documentation change.

## Completed foundation work

### Issue #3 — biometric enrollment session-key lifetime

[Issue #3](https://github.com/Yoslim1/Yonte/issues/3) is closed. The narrow biometric-enrollment lifetime fix is present at the architecture-foundation head in commit [6d8a591](https://github.com/Yoslim1/Yonte/commit/6d8a59133ea5d5388408cfdc9de54c19e938eb7b).

The fix is limited to preserving the operation-owned mutable session-key buffer until a terminal biometric callback, then zeroing it once. It does not redesign authentication, rotate cryptography, or change database schema.

## Pending integration and verification

### Issue #4 — Room schema migration baseline

[Issue #4](https://github.com/Yoslim1/Yonte/issues/4) remains open because [PR #29](https://github.com/Yoslim1/Yonte/pull/29) is still Draft and has not been merged into architecture-foundation.

PR #29 head [49893bb](https://github.com/Yoslim1/Yonte/commit/49893bb327c365d587bc77058728b8eebf64fcde) has canonical Android CI evidence from [run 197](https://github.com/Yoslim1/Yonte/actions/runs/35465467832): Kotlin/unit tests, debug compilation, architecture guard, changelog gate, encrypted database instrumentation, lint, and APK upload passed. This evidence applies only to that pull-request head until it is reviewed and integrated.

### Foundation CI blocker

The Android CI run triggered by this documentation branch, [run 198](https://github.com/Yoslim1/Yonte/actions/runs/35490342105), failed before Gradle, tests, lint, or instrumentation began. The Setup Android SDK action attempted to install the obsolete SDK package tools, which is no longer available.

This is a baseline CI-configuration/environment failure, not a documentation-content failure. PR #29 explicitly requests platform-tools and passed its own canonical CI, but that workflow correction is still pending integration. Do not treat architecture-foundation as currently CI-green until the applicable CI fix is reviewed and merged.

### Key-lifecycle hardening

[PR #28](https://github.com/Yoslim1/Yonte/pull/28) is open and Draft on branch fix/p0-key-lifecycle-hardening at [c2f7486](https://github.com/Yoslim1/Yonte/commit/c2f7486a8b98e8725af53f15cf7709ec8a914845). It must not be treated as shipped or as closing any security finding until independent review and final-SHA canonical CI evidence are complete.

## Foundation gate

[Issue #17](https://github.com/Yoslim1/Yonte/issues/17) remains open. Do not start a new product domain or surface, including Tasks, Calendar, Files, AI product features, habits, finance, or sync, before the relevant foundation gates are satisfied.

The next prerequisite work remains governed by the migration phases and the active security, data, backup, and boundary findings. A green build alone does not close the foundation gate.

## Documentation map

- [Documentation index](README.md)
- [Current foundation scope](architecture/FOUNDATION_STATUS.md)
- [Current security findings](security/reviews/CURRENT_FINDINGS.md)
- [Current backup findings](backup/reviews/CURRENT_FINDINGS.md)
- [Historical audits](history/audits/)

## Non-claims

This snapshot does not claim that a Draft pull request is merged, that main contains foundation work, or that a planned architecture is already implemented in product code.
