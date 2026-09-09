# Security Doctrine Index

## Purpose

Entry point for Yonte security policy. Detailed rules are intentionally split by responsibility.

## Read first

- `principles/CORE_INVARIANTS.md` — non-negotiable security rules.
- `principles/DATA_CLASSIFICATION.md` — PRIVATE/SENSITIVE/CRITICAL handling.
- `architecture/AUTHENTICATION_BOUNDARY.md` — proving user presence/session access.
- `architecture/AUTHORIZATION_BOUNDARY.md` — capability and consent decisions.
- `architecture/KEY_LIFECYCLE.md` — key ownership, wrapping, rotation, recovery.

## Change rule

Security mechanisms may evolve; the invariants above may change only through an explicit security architecture decision and focused review.
