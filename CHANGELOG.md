# Changelog

## 1.4.0 — Knote-inspired UX adaptation with original Yonte implementation

- Reworked the editor flow with a calmer title/body layout and a horizontal plain-text action strip for headings, bullets, tasks, and dividers.
- Fixed new-note autosave to allocate a stable draft UUID before the first asynchronous save, preventing duplicate rows during fast typing.
- Replaced the settings dialog with a full-screen sectioned settings flow covering Appearance, Data & backup, and Updates.
- Moved settings into an isolated `:feature:settings` module; the app root now composes Notes and Settings without feature-to-feature dependencies.
- Added a short UX adaptation record documenting what was learned from Knote and what was intentionally not copied.
- Added unit coverage for concrete editor actions; architecture, database, update, and debug build gates pass.

## 1.3.0 — UX/UI polish and reliable draft autosave

- Added an Android-style right-side drawer in RTL with a cleaner home surface.
- Replaced the ambiguous list/grid icon toggle with explicit stateful List and Grid chips.
- Added reliable draft autosave after a short debounce and final save on Back, Cancel, Save, and screen disposal.
- Preserved one draft identifier after the first autosave to avoid duplicate notes.
- Removed the unused legacy home implementation.
- Added a dedicated Yonte Design skill and documented the 2026 design principles.

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
