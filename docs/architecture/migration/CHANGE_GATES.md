# Architecture Change Gates

## Purpose

Define evidence required before non-trivial architectural changes are accepted.

## Standard gate

- current-state inspection.
- owning boundary identified.
- minimal change design.
- compatibility/failure analysis.
- focused tests.
- independent review when available.
- canonical CI evidence.

## Security/data/backup gate

Additionally require threat review, migration/recovery analysis, no-plaintext/no-destructive-fallback verification, and negative/fault tests appropriate to the change.

## Shared contract gate

Require owner, consumer impact map, version/deprecation strategy, and contract/integration tests.

## Production migration gate

Require old-data fixture coverage, interruption/failure behavior, post-migration validation, and safe abort behavior.

## Stop conditions

Do not land when:

- CI is red for a relevant code failure.
- a high/critical review finding remains unresolved.
- data-loss/recovery behavior is unknown.
- security behavior depends on an undocumented assumption.
- the change broadens scope only to make the implementation easier.
