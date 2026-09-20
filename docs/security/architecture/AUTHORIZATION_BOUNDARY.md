# Authorization Boundary

## Purpose

Define capability and consent decisions for users, AI, workers, restore operations, and future actors.

## Decision model

Authorization answers: `May actor A perform capability C on resource R now?`

A decision may be:

- `ALLOW`
- `DENY`
- `CONFIRMATION_REQUIRED`

## Ownership

- Security owns the authorization decision model, policy evaluation, confirmation rules, and policy versioning.
- Each bounded context owns the semantic capability vocabulary and resource semantics for that domain (for example Notes may define `notes.read` or `notes.update`).
- Global entity identity/reference semantics belong to the data/platform identity contract, not to Security.
- Security consumes stable actor/resource references and capability identifiers; it does not own Room entities, feature models, or persistence schemas.
- A future capability registry may aggregate descriptors for discovery without transferring capability ownership away from the defining domain.

## Required dimensions

- actor identity/type.
- capability identifier.
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

This boundary does not implement feature business rules, define feature-specific capability enums/classes, own global entity identity, or access databases directly.
