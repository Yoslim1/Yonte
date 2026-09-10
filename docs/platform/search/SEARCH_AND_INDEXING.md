# Search and Indexing Policy

## Purpose

Keep search fast and extensible without granting global components direct access to every feature table.

## Rules

- Each bounded context owns the authoritative data and feature-specific search semantics for its entities.
- Derived indexes are rebuildable and are never the only copy of user-owned content.
- Cross-feature/global search consumes explicit search-provider contracts returning stable entity references and permitted presentation metadata.
- AI/knowledge indexing follows the same ownership and permission boundaries; it does not crawl arbitrary database tables.
- Sensitive content is indexed only when the governing privacy/permission policy allows it.
- Index failure must degrade predictably to a safe fallback or clear unavailable state; it must not corrupt authoritative data.
- Reindex/invalidation follows entity revision/lifecycle changes, including deletion and privacy revocation.

## Performance

Add indexes based on measured query patterns and representative datasets, not speculative indexing of every field.
