# AI Context and Memory

## Purpose

Separate temporary reasoning context from durable AI memory and original user data.

## Concepts

### Source data

Canonical feature-owned content such as notes/tasks/files.

### Context

Temporary data supplied for the current request/session. It is not durable memory by default.

### Memory

Persisted AI-specific information intentionally retained for future use under explicit policy.

## Rules

- Sensitive context is non-persistent by default.
- Context does not silently become memory.
- Memory writes are permission/policy governed and attributable to source/provenance where derived.
- Deleting/changing a source triggers invalidation rules for dependent memories/derived artifacts.
- CRITICAL secrets are not normal AI memory content.
