# Failure Handling Policy

## Purpose

Define predictable behavior when known or unknown failures occur without attempting to enumerate every possible exception.

## Failure classes

- transient: may succeed later; bounded retry can be appropriate.
- permanent: retry cannot fix the current condition.
- user-action-required: needs permission, credential, storage choice, update, or confirmation.
- conflict/stale-state: requires re-read/reconcile before mutation.
- integrity/security: stop the sensitive operation and fail closed.
- unknown: preserve data, stop unsafe continuation, emit privacy-safe diagnostics.

## Rules

- Classify failures at the owning boundary and translate implementation exceptions into typed domain/platform failures.
- Retrying is a policy decision, never a generic catch-all response.
- Multi-step destructive or data-changing operations define atomicity, compensating/safe-abort behavior, and partial-state handling.
- Repeated delivery/retry paths are idempotent where practical.
- User messages state the safe next action without exposing internals or secrets.
- Correlation IDs connect diagnostics across boundaries without logging protected content.
- Unknown failures must not silently report success.

## Verification

Critical flows test representative transient, permanent, malformed-input, interruption, duplicate, and unknown-failure paths at the narrowest reliable layer.
