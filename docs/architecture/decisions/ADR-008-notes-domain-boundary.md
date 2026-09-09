# ADR-008: Notes Domain Boundary Migration

## Status

Accepted migration strategy.

## Context

Notes presentation currently depends directly on `NoteEntity` and the concrete database repository. The persistence implementation also owns Room transactions and manual FTS access inside `core:database`.

The target is to remove persistence representation from Notes/UI and Settings backup flows without creating a generic global domain bucket or forcing a premature data-module split that would add circular dependencies or transaction plumbing.

## Decision

Introduce `:domain:notes` as the first Notes bounded-context boundary.

It owns a small cohesive set of platform-neutral Notes semantics/contracts:

- `Note` — plain domain model with stable Notes fields; no Room annotations, Android types, SQLCipher types, or persistence-only `normalizedText`.
- `NotesRepository` — the interactive Notes contract required by Notes presentation/use cases.
- a narrow backup/snapshot port (for example `NotesBackupPort`) for backup/settings integration rather than granting those consumers all interactive Notes capabilities.

Dependency direction for this migration:

```text
:feature:notes --------> :domain:notes <-------- :core:database
:feature:settings -----> :domain:notes          (narrow backup port only)
       :app -----------> contracts + implementations as composition root
```

For this phase, `core:database` remains the Room/SQLCipher persistence adapter and performs `NoteEntity <-> Note` mapping internally. It also regenerates persistence-only normalized search text when writing restored/domain notes.

Do not introduce `:data:notes` merely to match a layered diagram. Reconsider extraction only when the persistence adapter can own a real independent responsibility without circular database/transaction abstractions.

## Migration constraints

- preserve IDs, timestamps, pin/archive/trash/search/autosave semantics.
- no Room schema version bump is required for this boundary-only migration.
- Notes presentation must stop importing `NoteEntity`, DAO, Room, SQLCipher, and concrete database repository types.
- Settings backup mapping must stop depending on `NoteEntity`/`ArabicNormalizer` in the same boundary sequence so semantic enforcement can become global.
- `:domain:notes` remains platform-neutral and does not depend on Android, Room, Hilt, or `:app`.
- application-level DI remains in `:app`.

## Verification

- entity/domain mapping round-trip tests, including timestamps and lifecycle flags.
- normalized search text regeneration coverage on persistence writes/restores.
- Notes ViewModel behavior tests using domain `Note` rather than Room entities.
- backup snapshot/restore mapping tests preserving existing backup compatibility.
- after migration, architecture enforcement rejects persistence types from feature presentation.
- canonical CI remains green.

## Consequences

- Notes regains semantic ownership without requiring a rewrite of encrypted storage.
- UI, AI, backup, and future integrations can depend on stable Notes contracts rather than Room shape.
- persistence extraction remains possible later without committing the platform to unnecessary module count today.
- the migration adds one justified domain module and explicit mapping code/tests.

## Rejected

- generic `core:domain` containing unrelated feature models.
- immediate `:data:notes` extraction solely for textbook layering.
- keeping `NoteEntity` as the application-wide Note model.
- changing the physical schema while performing this boundary migration.
