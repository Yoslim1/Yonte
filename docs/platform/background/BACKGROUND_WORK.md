# Background Work Policy

## Purpose

Keep WorkManager and other OS callbacks as thin adapters around testable application jobs.

## Rules

- OS adapters translate platform input/output; they do not assemble database, security, repository, or crypto graphs.
- Background work depends on one or a small number of high-level use-case contracts.
- Jobs are cancellation-aware and idempotent where retry can repeat work.
- Retryable, permanent, and user-action-required failures are distinguished explicitly.
- Retries are bounded and must not loop forever on incompatible schema, invalid credentials, or revoked permissions.
- Persistent work inputs contain identifiers/configuration references, not secrets or protected content unless unavoidable and explicitly secured.
- Feature behavior must remain usable when an optional background job is unavailable.

## Verification

Test the application job independently from WorkManager, then test only the adapter-to-result mapping at the platform boundary.
