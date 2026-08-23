# Yonte V2 Roadmap

## Product goal

Yonte V2 is not a feature-count exercise. Its goal is to make the notes experience feel continuous, calm, and predictable: capture should be immediate, search should be visible, organization should be lightweight, and every action should have clear feedback and a safe recovery path.

The `Yoslim1/knote` repository was inspected as a reference. Its useful patterns—pinned/recent grouping, visible search, contextual tag chips, quick filters, long-press selection, clear back behavior, and privacy-first backup—were reinterpreted for Yonte. No source code, assets, branding, or text was copied.

## What changed in the first V2 slice

The Yonte home surface now has a workspace-oriented header, visible search, tag chips derived from real note content, a pinned/recent split, a list/grid preference, clearer previews, relative timestamps, and actionable empty states. The settings action is functional and exposes theme and encrypted backup/restore actions. The editor accepts Android share text and has a clear save/cancel path.

## Delivery stages

| Stage | Scope | Quality gate | Output |
|---|---|---|---|
| V2.0 — Flow foundation | Search-first home, pinned/recent sections, tags, list/grid toggle, empty states, theme control, backup actions | No dead actions; Android API 26 build; Arabic/LTR smoke checks | Current implementation |
| V2.1 — Organization | Archive and trash destinations, restore actions, tag filtering, multi-select, undo snackbars | Every destructive action is reversible or explicitly confirmed | Next feature commit |
| V2.2 — Editor quality | Debounced autosave, keyboard-safe editor, checklist blocks, formatted preview, draft recovery after process recreation | No lost draft in interruption tests; editor state tests | Editor reliability release |
| V2.3 — Privacy and settings | Persisted theme/font scale/language, biometric lock gate, screenshot preference, privacy onboarding | Keystore and biometric instrumented tests; no secret logs | Privacy release |
| V2.4 — Search quality | FTS5 capability test, Arabic normalization improvements, highlighting, fallback benchmark | Search correctness corpus for Arabic/English; 1k/10k note performance | Search release |
| V2.5 — Android integration | Share receiver refinement, widget design, notification-ready contracts, file picker edge cases | Intent, process death, rotation, and backup round-trip tests | Android integration release |

## V3 and future modules

V3 will add habits and streaks only after the notes V2 flow is stable. Calendar integration follows as a separate Android module using CalendarContract and explicit user permission. Finance is V4 because it needs stronger data modeling, encryption policy, reports, and migration tests. AI is an abstraction layer after the local product has a stable domain model; it will not be coupled directly to the notes UI.

## Engineering process

Every feature follows the same sequence: write the user flow, define the state transitions, implement the repository contract, build the smallest usable UI, test the failure path, verify Android API 26 and a current Android version, then merge as a focused commit. No feature is considered complete if it only looks correct while its persistence, back navigation, or recovery behavior is missing.

Each release must pass compilation, unit tests, migration tests for the database version in scope, backup export/import round-trip, RTL/LTR checks, and a manual smoke flow: open app, create note, edit note, search note, pin note, archive or delete, restore, export, import, and receive shared text.

## Non-goals for V2

V2 will not add cloud sync, accounts, ads, analytics, or a large dashboard. It will not add finance, habits, calendar, or AI merely to make the product look larger. The app should feel smoother before it becomes broader.
