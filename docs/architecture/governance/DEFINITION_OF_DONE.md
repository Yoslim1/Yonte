# Definition of Done

## Purpose

Define the minimum evidence required before an engineering change can be called complete.

## Every Change

A change is not done until:

- ownership and scope are clear.
- implementation matches the stated intent.
- unrelated behavior is unchanged or explicitly justified.
- focused tests cover the changed behavior.
- relevant architecture/changelog checks pass.
- canonical CI evidence is green for the final head.
- documentation/ADR is updated when a durable rule or contract changed.

## Boundary Changes

Additionally require:

- dependency direction remains valid.
- contracts have explicit owners.
- presentation does not depend on persistence/infrastructure details.
- lifecycle and concurrency behavior are reviewed.
- compatibility with current consumers is demonstrated.

## Data/Security/Backup Changes

Additionally require:

- threat/failure analysis.
- safe failure behavior.
- no plaintext or secret-logging regression.
- migration/recovery behavior is known.
- negative/fault-path tests exist where meaningful.
- mutable secrets are cleared where practical.
- destructive fallback is not introduced for user-owned data.

## Platform Contracts

Additionally require:

- versioning/deprecation strategy.
- integration/contract tests.
- consumer migration path.
- no hidden feature-to-feature implementation dependency.

## Completion Rule

Passing compilation alone is never sufficient. A change is complete only when the implementation, tests, compatibility, security/data implications, and final CI evidence agree.
