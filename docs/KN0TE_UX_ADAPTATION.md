# Yonte UX adaptation note

Status: REFERENCE
Last reviewed: 2026-09-02

## Purpose

This document records how Yonte uses Knote as a behavioral and quality reference without cloning its implementation. Yonte remains a local-only Arabic/English Android notes product with its own visual language, copy, architecture, and data contracts.

## Reference idea and original Yonte implementation

| Area | Reference idea observed in Knote | Original Yonte adaptation |
|---|---|---|
| Editor | Keep title and body in one calm writing flow with predictable back behavior. | Yonte keeps its own Material 3 theme and labels, adds a quiet horizontal action strip, and saves by a stable draft UUID on edit, toolbar action, back, and disposal. |
| Autosave | Treat editing as a draft lifecycle rather than a save-button workflow. | Yonte uses `NotesViewModel` debounce plus final save on leave/disposal. New drafts receive a UUID before the first async write so rapid input cannot create duplicate rows. |
| Formatting | Offer a small, horizontally accessible set of common writing actions. | Yonte implements only concrete plain-text tokens: heading, bullet, task, and divider. There is no fake rich-text action or copied parser. |
| Settings | Use a full-screen section menu with nested pages and a predictable back path. | Yonte places settings in isolated `:feature:settings`, with Appearance, Data & backup, and Updates pages composed by the app root. |
| Home | Make search, quick filters, pinned content, and view mode easy to understand. | Yonte retains its own drawer, search, tag chips, pinned/recent grouping, and functional list/grid state while preserving its local-first identity. |

## What is intentionally not copied

No Knote source code, assets, logos, colors, resource names, or product copy are reused. The implementation is written against Yonte's existing Room entities, Core gateway contracts, Material 3 theme, and feature-isolation rules. Similarity is limited to general interaction principles that are common to modern note-taking software.

## Acceptance checks

The redesign is accepted only when a user can open a new note, type without pressing Save, leave and return to the same text, use the concrete toolbar actions, open the right-side drawer in RTL, reach full-screen settings, return from nested settings predictably, switch list/grid visibly, and run backup/update actions without notes-to-settings module coupling.
