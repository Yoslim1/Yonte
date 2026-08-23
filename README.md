# Yonte

Yonte is an Android-only personal workspace that starts with fast, private notes and is designed to grow into habits, projects, finance, and intelligent assistance.

## Current scope

Yonte V2 focuses on a smooth local notes workspace: title and body editing, Arabic/English-ready UI, RTL, pinning, archiving, trash, visible search, tag filtering, pinned/recent grouping, list/grid presentation, Android share-to-note intents, encrypted backup actions, and a modular foundation for future features.

Yonte targets **Android API 26 and above**. There is no iOS or web product target in this repository.

## Technology

- Kotlin and Jetpack Compose
- Material 3 with dynamic colors on supported Android versions
- Room over SQLite
- FTS5 capability detection with normalized-text fallback
- Feature-driven, multi-module Gradle structure
- Android Keystore foundation for AES-GCM encryption
- Storage Access Framework adapter for future backup/restore flows

## Modules

- `:app` — Android entry point and platform intents.
- `:core:database` — Room entities, migrations, Arabic normalization, search repository.
- `:core:security` — Keystore-backed encryption foundation and biometric dependency.
- `:core:backup` — Versioned encrypted backup envelope.
- `:core:navigation` — Feature navigation contracts.
- `:core:designsystem` — Shared Compose theme.
- `:feature:notes` — Notes UI and presentation logic.

## Build

Open the repository in Android Studio with JDK 17, or run:

```bash
./gradlew :app:assembleDebug
```

The GitHub Actions workflow builds the debug APK on every push to `main` and every pull request.

## Data safety

The application is local-first and does not require an account or a server. Plain SQLite is not represented as full-database encryption. The security and backup modules are deliberately separated so that SQLCipher or field-level encryption can be introduced only after a tested proof of concept.
