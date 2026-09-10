# Restore Safety Policy

## Purpose

Define non-negotiable behavior for importing/restoring user data.

## Restore pipeline

1. Bound input size/resource use.
2. Validate outer format and supported version.
3. Authenticate/decrypt.
4. Validate integrity and inner schema version.
5. Parse into isolated staging representation.
6. Validate every record and relationship.
7. Build conflict/merge plan.
8. Apply atomically to live storage.
9. Verify post-restore invariants.
10. Mark success only after verification.

## Invariants

- Failed restore never corrupts or partially overwrites live data.
- No destructive migration is used as recovery.
- Existing live state remains available if pre-commit validation fails.
- Unsupported newer formats fail explicitly.
- Recovery does not silently weaken encryption or permissions.

## Conflict policy requirement

Restore mode must be explicit: replace, merge, or selected import. Each mode needs deterministic ID/revision conflict semantics before implementation.

## Verification

Test failure at each pipeline boundary, including process interruption and storage errors.
