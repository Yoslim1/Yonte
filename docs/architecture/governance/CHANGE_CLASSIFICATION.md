# Change Classification

## Purpose

Classify engineering changes before implementation so the correct review, migration, and verification gates are applied consistently.

## Class A — Local Change

A change is local when all are true:

- one owning boundary.
- no public/shared contract change.
- no database schema or migration change.
- no security, backup, update-trust, or permission-model change.
- no cross-feature dependency change.
- no persistent-data compatibility impact.

Examples: isolated UI polish, internal refactor with unchanged contract, focused test-fixture correction.

## Class B — Boundary Change

A change is a boundary change when it alters dependency direction, ownership, lifecycle, or a feature/platform contract without changing persisted user data.

Requires:

- impact map.
- architecture review.
- compatibility analysis.
- focused contract/integration tests.
- canonical CI.

## Class C — Data/Security Critical Change

A change is critical when it affects any of:

- authentication or authorization.
- key lifecycle or cryptography.
- backup/restore/recovery.
- database schema/migration/data ownership.
- update trust chain.
- AI permissions or sensitive-data handling.

Requires the security/data/backup gate in addition to normal architecture review.

## Class D — Platform Evolution

A change is platform evolution when it introduces or versions a shared platform primitive such as global identity, relations, event contracts, capability registry, or cross-feature protocol.

Requires:

- explicit contract owner.
- consumer impact map.
- version/deprecation plan.
- rollback/safe-abort strategy.
- integration/compatibility tests.
- architecture decision record when the decision is durable.

## Rule

Classify by the highest-impact effect, not by line count. A one-line crypto or migration change may be Class C while a large mechanical UI split may remain Class A.
