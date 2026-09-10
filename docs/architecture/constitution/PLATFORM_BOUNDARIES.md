# Platform Boundaries

## Purpose

Define the shared boundaries that connect sovereign features into one coherent product.

## Boundaries

### Identity

Cross-feature references use stable entity ID, entity type, owner/context, and revision/provenance where required.

### Data

One encrypted physical database may host multiple feature-owned schemas/tables. Shared physical storage does not grant shared direct table access.

### Security

Authentication, authorization, cryptography, key lifecycle, privacy policy, and audit are platform capabilities exposed through narrow contracts.

### Integration

- Command: request an action.
- Query: request information.
- Event: announce an already completed state change.

Events carry references/metadata by default, not protected content.

### AI

AI uses permission/consent and knowledge/action gateways. It may understand semantic types such as Note/Task but never depends on persistence implementation.

### Composition

Concrete implementations are wired at the application composition root through dependency injection. Workers and UI should consume high-level use cases rather than construct infrastructure graphs.

## Non-goals

A generic all-powerful Core API that every feature can use to bypass ownership.
