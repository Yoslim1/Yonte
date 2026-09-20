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
| architecture-foundation | Integration baseline for completed foundation work. It contains the closed Issue #3 fix and the merged Room schema baseline. | [617465e](https://github.com/Yoslim1/Yonte/commit/617465e1b24bb8bb0d786d4960016efae4261bd5) |

Foundation work targets architecture-foundation through focused pull requests. No work is merged directly into main by this documentation change.

## Completed foundation work

### Issue #3 — biometric enrollment session-key lifetime

[Issue #3](https://github.com/Yoslim1/Yonte/issues/3) is closed. The narrow biometric-enrollment lifetime fix is present on architecture-foundation in commit [6d8a591](https://github.com/Yoslim1/Yonte/commit/6d8a59133ea5d5388408cfdc9de54c19e938eb7b).

The fix is limited to preserving the operation-owned mutable session-key buffer until a terminal biometric callback, then zeroing it once. It does not redesign authentication, rotate cryptography, or change database schema.

### Room schema migration baseline

[PR #29](https://github.com/Yoslim1/Yonte/pull/29) merged into architecture-foundation at [617465e](https://github.com/Yoslim1/Yonte/commit/617465e1b24bb8bb0d786d4960016efae4261bd5).

The foundation now contains the committed Room v1 schema, KSP schema-export configuration, migration-test support, and explicit platform-tools setup. The final PR head passed [Android CI run 197](https://github.com/Yoslim1/Yonte/actions/runs/35465467832), including Kotlin/unit tests, debug compilation, architecture/changelog gates, encrypted database instrumentation, lint, and APK upload.

[Issue #4](https://github.com/Yoslim1/Yonte/issues/4) is closed with completion evidence after the post-merge foundation verification.

## Pending review and verification

### Documentation current-state cleanup

[PR #30](https://github.com/Yoslim1/Yonte/pull/30) is the documentation cleanup proposal. It must be reviewed and verified against the updated architecture-foundation base before merge.

The obsolete Android SDK tools-package failure seen on the earlier documentation-branch run is corrected in the merged PR #29 workflow. A fresh CI result on the updated documentation PR is required before calling its verification green.

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
