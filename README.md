# Yonte

Yonte is an Android-only personal workspace for private, local-first notes. The repository currently implements encrypted local notes, organization, search, backup support, and an update-checking flow without an account or required server.

## Current scope

The implemented notes workspace supports title and body editing, Arabic/English-ready UI, RTL-aware layouts, pinning, archiving, trash, search, tag filtering, pinned/recent grouping, list/grid presentation, Android share-to-note intents, encrypted backup actions, and a manual update center that verifies APK SHA-256 before installation. These capabilities are local application behavior; future roadmap items are not treated as current features.

Yonte targets **Android API 26 and above**. There is no iOS or web product target in this repository.

## Technology and security model

The application uses Kotlin, Jetpack Compose, Material 3, Hilt, Room, SQLCipher, Android Keystore-backed key wrapping, Argon2-based passphrase derivation, and Kotlin coroutines. Room persists notes through SQLCipher's encrypted SQLite driver; there is no plaintext database fallback. The passphrase-derived session key is available only after onboarding or unlock and is never persisted as plaintext. The process-local database singleton is bound to an in-memory SHA-256 digest of the active session key, so it is not reused for a different key, and it can be explicitly closed before a new session is opened.

The security model also includes PIN and biometric unlock paths backed by the existing local key-management components. Backup and restore use a separate encrypted envelope and are not a substitute for the database encryption boundary.

## Modules

| Module | Responsibility |
|---|---|
| `:app` | Android entry point, Hilt wiring, lifecycle, platform intents, and navigation host. |
| `:core:database` | SQLCipher-backed Room database, entities, DAOs, repositories, migrations/schema configuration, and Arabic normalization/search. |
| `:core:security` | Argon2 key derivation, Android Keystore-backed wrapping, PIN state, and biometric support. |
| `:core:backup` | Versioned encrypted backup envelope and scheduled backup integration. |
| `:core:navigation` | Shared feature navigation contracts. |
| `:core:designsystem` | Shared Compose theme and design tokens. |
| `:core:update` | Update checking, download, SHA-256 verification, and installation handoff. |
| `:feature:notes` | Notes UI, editor, search, and presentation logic. |
| `:feature:settings` | Settings UI and backup/update configuration behavior. |
| `:feature:onboarding` | First-run onboarding and unlock-flow UI. |

Feature modules do not depend on other feature modules, and core modules do not depend on the application or feature modules. The executable architecture guard is `python3 tools/check_architecture.py`.

## Build and CI

Open the repository in Android Studio with JDK 17. The repository's version catalog at [`gradle/libs.versions.toml`](gradle/libs.versions.toml) is the source of truth for dependency and plugin versions. The local command for a developer build is:

```bash
./gradlew :app:assembleDebug
```

GitHub Actions is the authoritative verification environment for repository changes. The workflow runs the architecture guard, JVM tests for the configured modules, a debug APK compilation, and app lint, then uploads the debug APK as an artifact. Instrumented database tests require an Android device/emulator and are not represented as passing merely because JVM CI is green.

The signed-release job is conditional on a version tag, the `YONTE_SIGNED_RELEASE_ENABLED` repository variable, and the configured signing secrets. It restores signing material only in the CI workspace, verifies the APK signature with `apksigner`, and uploads the resulting release artifact. No signing material belongs in the repository.

## Documentation

The modularization rules are documented in [`docs/FEATURE_ISOLATION_BLUEPRINT.md`](docs/FEATURE_ISOLATION_BLUEPRINT.md). Roadmaps and design/reference notes are planning or historical material and must not override the current source code or Gradle configuration.

## Data safety

Yonte is local-first and does not require an account or a server. Protected application data is stored in an encrypted SQLCipher database. Schema downgrades do not use destructive migration fallback; if a downgrade cannot be migrated safely, it must fail visibly rather than silently discard user data. Backup data is separately encrypted and is handled through the backup module.
