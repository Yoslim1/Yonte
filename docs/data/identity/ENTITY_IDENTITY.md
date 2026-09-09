# Global Entity Identity

## Purpose

Define the stable identity used to reference user-owned objects across features without leaking persistence models.

## Required reference

A cross-feature entity reference contains at minimum:

- globally stable entity ID.
- semantic entity type (`NOTE`, `TASK`, `EVENT`, etc.).
- owning feature/domain.
- owner/workspace context when applicable.
- revision/version when consistency matters.

## Rules

- Entity IDs survive internal storage refactors.
- Cross-feature contracts pass identity/domain models, never Room entities.
- Semantic type is allowed; persistence implementation is not.
- IDs must be generated without dependence on mutable display fields.

## Non-goal

This does not require a single giant table containing every feature's content.
