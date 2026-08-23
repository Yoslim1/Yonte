# Changelog

## 1.2.0 — Secure update center

- Added a manual update checker in Settings.
- Added a public metadata-only update feed; the source repository remains private.
- Added version comparison, minimum SDK validation, APK download, SHA-256 verification, and Android installer handoff.
- No background polling, analytics, account, or user-data upload is used.

## 1.1.0 — V2 flow foundation

- Added a workspace-oriented notes home with pinned and recent sections.
- Added tag chips derived from note content and lightweight tag filtering.
- Added a list/grid presentation preference for the notes surface.
- Improved note previews, relative timestamps, and empty states.
- Added working theme control and encrypted backup/restore actions in settings.
- Added a reference analysis document for `Yoslim1/knote`; no code or assets were copied.
- Added the V2 roadmap and quality gates.

## 1.0.0 — Core foundation

- Android-native Kotlin and Jetpack Compose application.
- Room-backed local notes repository with Android API 26 minimum.
- Arabic normalization and FTS5 capability with a safe fallback search path.
- Android share-to-note intent for incoming plain text.
- Android Keystore AES-GCM foundation and versioned backup envelope.
- Private GitHub release tag `v1.0.0`.
