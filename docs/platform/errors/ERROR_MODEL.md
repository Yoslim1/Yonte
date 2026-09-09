# Canonical Error Model

## Purpose

Provide typed, diagnosable failures without leaking implementation exceptions or user content across boundaries.

## Error contract

A platform/domain failure should expose where applicable:

- stable error code.
- category/severity.
- retryable flag.
- safe user-facing message/action.
- correlation ID.
- structured non-sensitive metadata.

Examples: `SECURITY.AUTH_REQUIRED`, `DATA.CONFLICT`, `BACKUP.INTEGRITY_FAILED`, `STORAGE.NO_SPACE`, `AI.PERMISSION_REQUIRED`.

## Rules

- UI does not depend on raw SQL/HTTP/crypto exceptions.
- Unknown failures fail safely and preserve data.
- Retry policy is explicit; do not blindly retry permanent failures.
- Diagnostics never contain protected content/secrets.
