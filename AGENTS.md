# Yonte — Agent Policy

## Project Overview

Yonte is an Android notes application built with Kotlin and Jetpack Compose. It is Android-only; all shared code remains in Kotlin modules.

## Tech Stack

- **Language:** Kotlin 2.0.21, JVM target 17
- **UI:** Jetpack Compose with Material 3
- **DI:** Hilt 2.54 (KSP annotation processing)
- **Database:** Room 2.6.1 with SQLCipher 4.5.4 encryption
- **Build:** Gradle with version catalog (`gradle/libs.versions.toml`), AGP 8.7.3
- **Min SDK:** 26, Target/Compile SDK: 35

## Module Structure

```
:app                    — Application entry point, Hilt wiring, navigation host
:core:database          — Room database, DAO, repository, SQLCipher encryption
:core:security          — Biometric auth, Argon2 key derivation, encryption manager
:core:backup            — Backup/restore via kotlinx.serialization
:core:navigation        — Navigation definitions
:core:designsystem      — YonteTheme and shared Compose theme tokens
:core:update            — Update gateway and service
:feature:notes          — Notes CRUD UI, ViewModel, search, editor
:feature:settings       — Settings screen
:feature:onboarding     — First-run onboarding flow
```

## Architecture Rules

Feature isolation is enforced by `tools/check_architecture.py` in CI:

- Feature modules must NOT import from other feature modules.
- Feature modules must NOT import from `:app`.
- Core modules must NOT import from `:app` or any `:feature:*` module.
- Feature build files must NOT declare Gradle dependencies on other feature modules.

Respect these dependency directions. The architecture check is authoritative when run in GitHub Actions. Run the Python architecture check locally only when the user explicitly allows local verification; local Gradle and Android build commands remain prohibited under the current policy.

## Dependency Direction

```
:app ──→ :core:database, :core:security, :core:backup, :core:navigation, :core:designsystem, :core:update
:app ──→ :feature:notes, :feature:settings, :feature:onboarding
:feature:notes ──→ :core:database, :core:backup, :core:security, :core:update, :core:designsystem, :core:navigation
:feature:settings ──→ :core:database, :core:backup, :core:update, :core:designsystem
:feature:onboarding ──→ (no core dependencies)
:core:backup ──→ :core:security
```

## Coding Conventions

- Declare everything at the lowest visibility that still compiles. Start `private`; widen to `internal` only when another file in the same module needs it; widen to `public` only when a different module actually consumes it.
- Use strict Kotlin typing. Avoid `Any`, unsafe casts, reflection, and unchecked operations unless clearly justified.
- Keep Compose state ownership explicit. Avoid side effects directly inside composable bodies.
- Use `remember`, `derivedStateOf`, and effect APIs only when justified by the actual state flow.
- Respect lifecycle, coroutine, threading, cancellation, state, and configuration-change semantics.
- Follow Material 3 and the existing `YonteTheme` conventions.
- Keep UI, domain, and data responsibilities separated. The repository layer is the error boundary.
- Never hard-code secrets, tokens, credentials, or signing material.
- Validate untrusted input at system boundaries.

## Key Dependencies (from version catalog)

| Library | Version |
|---|---|
| Compose BOM | 2024.12.01 |
| Room | 2.6.1 |
| Hilt | 2.54 |
| Navigation Compose | 2.8.5 |
| SQLCipher | 4.5.4 |
| Biometric | 1.1.0 |
| Argon2kt | 1.5.0 |
| Coroutines | 1.9.0 |
| Serialization | 1.7.3 |

## CI-Only Build and Test Commands

CI runs on GitHub Actions (`.github/workflows/android.yml`). The commands below document what GitHub Actions runs; they are not instructions to run locally. Local Gradle, Android builds, tests, and lint are prohibited unless the user explicitly changes that policy.

**CI verification steps:**
1. `python3 tools/check_architecture.py` — enforce feature isolation
2. `./gradlew :core:update:test :core:database:test :core:security:test :core:backup:test :feature:notes:test :feature:onboarding:test :feature:settings:test :app:assembleDebug --stacktrace` — compile and test
3. `./gradlew :app:lintDebug --stacktrace` — lint

**Signed release** (gated by `YONTE_SIGNED_RELEASE_ENABLED=true` repo variable and version tags):
- `./gradlew :app:assembleRelease --stacktrace`
- APK signature verification via `apksigner`

## CI/CD Policy

- Build, test, lint, and verification happen through GitHub Actions only.
- Never push, merge, publish, release, or deploy without explicit user approval.
- After five consecutive CI failures for the same task, stop and report to the user.
- Distinguish code failures from infrastructure or environment failures.

## Git Policy

- Never push, merge, release, or rewrite history automatically.
- Do not create commits merely because a task is complete unless the user explicitly requests a commit.
- Keep commits focused when the user later requests commits.
- Before destructive Git operations, obtain explicit user approval.

## Scope and Change Control

- Modify only files necessary for the user-requested task.
- Never make unrelated cleanup changes.
- Preserve existing behavior unless behavior changes are explicitly requested.
- Ask for user approval before a proposed change materially expands scope, changes architecture, introduces a dependency, changes public behavior, or requires a destructive operation.

## Verification

- Evidence before claims, always. Do not claim builds, tests, or checks passed without actual evidence.
- Use `verification-before-completion` when claiming work is done.
- GitHub Actions is the authoritative verification environment.
- Local Gradle and Android builds are prohibited by this project policy.

## App-Specific Patterns

- **Encryption-first onboarding:** The database key is derived from a user passphrase via Argon2 and stored in `LocalKeyManager`. The `YonteDatabase` Hilt provider requires a cached session key; first run must never reach the database.
- **Lazy injection:** `NoteRepository` is injected as `Lazy<NoteRepository>` in `MainActivity` so the database is not opened before onboarding/unlock.
- **FTS5 search with Arabic normalization:** Notes use a custom `ArabicNormalizer` for full-text search fallback on SQLite builds without FTS5.
- **Autosave with debounce:** The notes ViewModel debounces saves at 350ms to avoid excessive writes.

## Installed Skills (relevant to this project)

The following skills from `~/.config/opencode/skills` are verified installed and relevant to Yonte work:

| Skill | When to use |
|---|---|
| `clean-code-guard` | Review generated/changed production code before shipping |
| `test-guard` | Review generated/changed test code before shipping |
| `verification-before-completion` | Before claiming work is complete |
| `incremental-implementation` | Multi-file changes, large features |
| `source-driven-development` | Framework-specific code backed by official docs |
| `modularization` | Multi-module visibility decisions |
| `android-dev` | Baseline for all Android/Kotlin work |
| `compose` | Compose state, composition, animations, performance |
| `material-3` | Material Design 3 theming and components |
| `chrisbanes-compose-performance` | Compose recomposition cost, stability reports |
| `ci-cd-and-automation` | CI/CD pipeline setup and quality gates |
| `git-workflow-and-versioning` | Commits, branching, releases |
| `performance-optimization` | Application performance measurement and fixes |
| `observability-and-instrumentation` | Logging, metrics, tracing |
| `browser-testing-with-devtools` | Browser-based testing with Chrome DevTools |
| `frontend-ui-engineering` | Production-quality UI engineering |
| `android-data-layer` | Repository pattern, Room, error propagation |
| `android-gradle-logic` | Build logic, convention plugins, version catalogs |
| `android-ninja` | Compose screens, ViewModels, Room, Hilt, Navigation |
| `android-testing` | Android/KMP test patterns and traps |

Do not list every installed skill. Only the ones above are relevant to this project's stack and workflow.

Skills provide specialized instructions and workflows for specific tasks.
Use OpenCode's native skill tool to load a relevant skill when a task matches its description. Verify the corresponding SKILL.md when necessary, and never claim that a skill was used unless it was actually loaded and applied.

## Subagent Workflow

For non-trivial software tasks, use this preferred sequence:

`@implementer` → `@reviewer` → `@tester`

The `@implementer` writes the approved change within the requested scope. The `@reviewer` critically reviews the change and uses `clean-code-guard` and `test-guard` when they are installed, read, and relevant. The `@tester` inspects GitHub Actions results and verification evidence without running local Gradle or Android builds.

These names refer to real subagents only when matching agent definitions exist in `.opencode/agents/` or `~/.config/opencode/agents/`. AGENTS.md does not create subagents by itself. If a required subagent is unavailable, report that blocker instead of pretending it was invoked.

The primary agent must not skip a stage without explaining why. No subagent may push, merge, release, deploy, or modify Git history automatically. After five consecutive CI failures for the same task or change, stop and report to the user; do not perform a sixth retry.
