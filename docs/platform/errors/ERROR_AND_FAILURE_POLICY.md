# Error and Failure Policy

## Purpose

Define a canonical error contract and predictable failure behavior without leaking implementation exceptions, user content, or secrets across boundaries.

## Canonical error contract

A platform/domain failure should expose where applicable:

- stable error code.
- category/severity.
- retryable flag.
- safe user-facing message/action.
- correlation ID.
- structured non-sensitive metadata.

Examples include `SECURITY.AUTH_REQUIRED`, `DATA.CONFLICT`, `BACKUP.INTEGRITY_FAILED`, `STORAGE.NO_SPACE`, and `AI.PERMISSION_REQUIRED`.

Raw SQL, HTTP, crypto, provider, or filesystem exceptions are implementation details and should be translated at the owning boundary.

## Failure classes

- **Transient** — may succeed later; bounded retry can be appropriate.
- **Permanent** — retry cannot fix the current condition.
- **User action required** — needs permission, credential, storage choice, update, or confirmation.
- **Conflict / stale state** — requires re-read or reconciliation before mutation.
- **Integrity / security** — stop the sensitive operation and fail closed.
- **Unknown** — preserve data, stop unsafe continuation, and emit privacy-safe diagnostics.

## Handling rules

- Retrying is a policy decision, never a generic catch-all response.
- Unknown failures never silently report success.
- UI depends on typed platform/domain failures, not raw implementation exceptions.
- Multi-step destructive or data-changing operations define atomicity, safe-abort/compensation, and partial-state behavior.
- Repeated delivery/retry paths are idempotent where practical.
- User messages state the safe next action without exposing internals or secrets.
- Correlation IDs connect diagnostics across boundaries without logging protected content.
- Diagnostics never contain passphrases, keys, tokens, note content, or equivalent protected payloads.

## Verification

Critical flows test representative transient, permanent, malformed-input, interruption, duplicate, conflict, integrity, and unknown-failure paths at the narrowest reliable layer.

A passing happy-path test does not prove failure safety.
