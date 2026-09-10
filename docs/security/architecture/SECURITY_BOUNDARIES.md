# Security Boundary Map

## Purpose

Navigate the authoritative security boundaries without duplicating their rules.

## Boundaries

- `AUTHENTICATION_BOUNDARY.md` — proving eligibility to establish/open a protected session.
- `AUTHORIZATION_BOUNDARY.md` — deciding whether an actor may perform a capability on a resource now.
- `KEY_LIFECYCLE.md` — purpose-specific key ownership, wrapping, rotation, migration, and recovery.
- `../principles/DATA_CLASSIFICATION.md` — handling requirements that follow sensitive data across boundaries.

## Global rule

Authentication, authorization, and key wrapping are distinct responsibilities. Their durable separation is recorded in `../../architecture/decisions/ADR-007-security-boundary-separation.md`.

## Non-goal

This document defines no additional security policy; detailed rules belong to the linked authoritative documents.
