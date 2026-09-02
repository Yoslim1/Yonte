# Reference analysis: knote

Status: REFERENCE
Last reviewed: 2026-09-02

The available repository was `Yoslim1/knote`, not a repository named `Konte`. It was inspected as a reference only. No source code, assets, or branding are copied into Yonte.

## Useful product ideas to reinterpret

Knote makes the notes surface feel like a workspace rather than a plain database list. The strongest ideas worth reinterpreting are: a visible search entry point, pinned content separated from the rest, tags that reflect the current dataset, quick filters that disappear when they have no results, long-press selection, predictable back behavior, readable note previews, and a settings area that treats privacy and personalization as first-class concerns.

The reference also demonstrates a strong offline product promise: no accounts, no cloud requirement, no advertising, no tracking, and deliberate backup/export. Yonte will adopt the privacy principle, but its visual language, navigation, data model, wording, and implementation will be original.

## What Yonte will not copy

Yonte will not copy Knote's code, package names, layouts line-for-line, README text, visual assets, feature wording, license headers, or security implementation. Its own identity remains Yonte, with a simpler V1 and a different V2 information architecture.

## V2 direction for Yonte

V2 will focus on flow and confidence before adding a large number of modules. The home surface will have a clear capture action, a search-first interaction, pinned and recent sections, tag chips derived from real notes, and empty states that explain the next useful action. Editing will use an explicit save state, keyboard-safe layout, autosave debounce, and a single predictable back path.

The V2 roadmap will add: a proper notes workspace shell, list/grid preference, tag filters, archive/trash destinations, multi-select actions, haptic and visual feedback, persisted appearance/font settings, Android locale selection, a first-run privacy explanation, and stronger backup/restore feedback. Habits, calendar, finance, and AI remain future feature modules and will not be allowed to clutter the notes V2 experience.

## Quality gates

Every V2 feature must have a complete flow, no empty click handlers, state restoration after rotation/process recreation where relevant, tested RTL and LTR text, Android API 26 compatibility, and a test or verification note. Performance will be checked with large note sets before adding visual polish.
