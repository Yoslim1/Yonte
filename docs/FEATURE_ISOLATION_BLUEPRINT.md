# Yonte Feature Isolation Blueprint

Status: ACTIVE
Last reviewed: 2026-09-02

## Status

This document is a mandatory engineering rule for Yonte V2 and all later releases. It applies to production code, tests, Gradle dependencies, navigation, state ownership, and shared UI decisions.

## Dependency direction

Features are horizontal peers. A feature may depend on `:core:*` contracts and on its own package, but it must never import another feature or depend on another `:feature:*` Gradle module. Core modules must not import `:app` or any feature. The `:app` module is the composition root: it owns Android entry points and wires implementations to Core interfaces.

The current dependency direction is:

```text
:app
 ├── :feature:notes
 ├── :core:database
 ├── :core:backup
 ├── :core:security
 ├── :core:update
 ├── :core:navigation
 └── :core:designsystem

:feature:notes ──> :core contracts and data APIs only
:core:backup ───> :core:security
:core:* ────────> no :app and no :feature:* imports
```

## Dependency injection

Hilt is the application composition mechanism. `YonteAppModule` provides the database, repository, encryption manager, `BackupGateway`, and `UpdateGateway`. Features receive Core interfaces rather than constructing service implementations. A feature must not instantiate a service that belongs to the application graph.

## Navigation

Features emit navigation intents through interfaces defined in `:core:navigation`. A feature must not name another feature route. The app-level navigation host owns route registration and decides which destination is available. Adding or deleting a feature therefore changes composition, not the internal code of peer features.

## State ownership and UDF

Each feature owns its ViewModel, state, intents, and reducer-like transitions. `NotesViewModel` is a Hilt ViewModel and owns only note search and note actions. No global ViewModel or cross-feature mutable state is permitted. UI reads state and emits events; repositories perform persistence; results flow back as state.

## Shared UI

Reusable visual primitives belong in `:core:designsystem`. Feature screens compose those primitives and may add feature-specific composition, but they must not duplicate common buttons, cards, typography tokens, spacing, or theme behavior.

## Deletion test

The architecture guard checks for feature-to-feature imports, feature-to-app imports, core-to-app imports, core-to-feature imports, and feature-to-feature Gradle edges. A future feature is accepted only if the guard passes and removing the feature does not require edits inside peer features or Core. The app composition root may need a deliberate route-registration edit when a feature is removed; that is an allowed boundary change, not peer coupling.

## CI gate

Every push and pull request runs `tools/check_architecture.py` before the Android build. The gate must pass alongside unit tests, migration tests, update verification tests, and the Android build. Any violation blocks the change until the dependency direction is restored.
