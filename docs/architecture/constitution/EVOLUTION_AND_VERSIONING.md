# Evolution and Versioning

## Purpose

Define how Yonte evolves without breaking data, contracts, security, or old backups.

## Independent versions

Track independently where applicable:

- app/release version.
- database schema version.
- event contract version.
- backup format/schema version.
- persistent security/key-envelope version.
- authorization/AI policy version.

## Contract evolution

Use expand -> migrate consumers -> deprecate -> remove. Do not replace a shared contract in-place and repair breakage reactively.

## Persistent-format rule

Readers may support defined legacy versions; writers produce the current version. Migration must verify replacement state before old recoverable state is discarded.

## Data evolution

- No destructive production migration shortcut.
- Schema changes require migration strategy and tests.
- Entity revisions support concurrency/conflict detection.
- Future sync must preserve stable identity, deletion/tombstone semantics, and explicit conflict policy.

## Deprecation ownership

Every shared contract must have a clear owner responsible for compatibility, deprecation communication, and removal criteria.
