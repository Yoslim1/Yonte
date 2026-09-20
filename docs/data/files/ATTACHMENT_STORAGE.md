# Attachment Storage Policy

## Purpose

Define ownership and protection for future files, images, audio, and other binary user content that should not live as arbitrary blobs inside feature tables.

## Rules

- The owning feature controls attachment semantics and lifecycle.
- Binary content is stored through a dedicated encrypted-storage boundary when persistence is required; database rows keep metadata/stable references rather than duplicating large content.
- Attachment IDs are stable references and never imply a raw filesystem path.
- Delete/archive/export/restore behavior is defined with the owning entity to prevent orphaned or leaked files.
- Backup and restore include attachment integrity/version metadata when attachments become supported.
- Events, audit logs, and diagnostics carry attachment references/metadata, not protected binary content.
- File type, size, and parsing limits are validated before expensive processing.

## Non-goal

Do not introduce attachment infrastructure until a feature requires it. This policy defines the boundary so future support does not bypass data ownership or security.
