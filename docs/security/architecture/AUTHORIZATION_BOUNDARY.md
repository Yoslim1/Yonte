# Authorization Boundary

## Purpose

Define capability and consent decisions for users, AI, workers, restore operations, and future actors.

## Decision model

Authorization answers: `May actor A perform capability C on resource R now?`

A decision may be:

- `ALLOW`
- `DENY`
- `CONFIRMATION_REQUIRED`

## Required dimensions

- actor identity/type.
- capability (`notes.read`, `notes.update`, `notes.delete`, etc.).
- resource/entity scope.
- data classification.
- standing user permission.
- operation risk.
- current policy version.

## AI rules

- AI never receives implicit superuser access.
- Standing permission does not remove mandatory confirmation for destructive/high-risk operations.
- Delete and destructive bulk changes require confirmation.
- Sensitive external processing requires explicit policy/confirmation.
- Temporary sensitive context does not automatically become AI memory.

## Audit

Record actor, capability, entity reference, decision, policy version, timestamp, correlation ID, and safe error code only.

## Non-goals

This file does not implement feature business rules or database access.
