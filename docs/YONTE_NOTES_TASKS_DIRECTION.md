# Yonte notes and tasks direction

## Product identity

Yonte remains the product name, Android package, repository, and local-first application. The attached Knote documents are reference material only. Yonte will not copy Knote's name, palette, illustrations, wording, navigation labels, or implementation. The target is a **private personal workspace** where long-form notes and actionable tasks can coexist without making the home screen feel like a dashboard.

## Yonte information model

| Content | Primary job | Visual treatment in Yonte |
|---|---|---|
| Note | Capture an idea, explanation, journal entry, or reference. | Calm writing canvas, larger title, readable body, optional tags, pinning, and local autosave. |
| Task | Capture one actionable item with a completion state. | Fast entry row or task line with a clear checkbox, compact metadata, and completed-state strike-through. |
| Collection | Help the user find content without forcing a rigid folder hierarchy. | Search, pinned/recent grouping, local tags, and optional filters. |

The current database remains notes-first. Until a task entity and migration contract are deliberately approved, task-like lines must not be presented as a fully separate persisted feature. The first safe implementation is to support task formatting inside the editor and design the interaction boundary so a future `TaskEntity` can be added without coupling features.

## Home direction

The first screen should answer three questions immediately: what is here, how can I find it, and how can I add something now. The hierarchy is Yonte title and short context, search, lightweight filters, a pinned/recent content area, and one prominent create action. Notes and tasks should not be mixed through decorative labels; their distinction should come from content structure and clear interaction states.

## Editor direction

The editor is a writing surface rather than a form. Title and body use a borderless layout, the keyboard does not hide the active content, autosave is visible but quiet, and the toolbar contains only actions that change the text or task state. A task action creates a real checkable text token today; future task persistence must be introduced through a Core contract and migration, not a feature-local shortcut.

## Settings direction

Settings remain a full-screen flow owned by the isolated settings feature. Appearance, local data/backup, and manual updates are valid today. Notifications, cloud sync, calendar, biometrics, and attachments may be future roadmap items but must not be shown as available controls until their contracts and behavior exist.

## Acceptance boundary

The Yonte redesign is successful when it feels visually distinct from Knote, opens directly into a calm notes workspace, supports fast writing and task-like capture without fake controls, preserves drafts without a Save button, remains correct in Arabic RTL and English LTR, and leaves a clean architectural path for a future task module.
