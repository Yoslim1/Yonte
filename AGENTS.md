Yonte — Agent Operating Policy

1. Mission

Yonte is a production Android application. Your responsibility is to make correct, secure, maintainable, minimal-scope changes while preserving the project's existing architecture, behavior, data integrity, and security guarantees.

Optimize for:

1. Correctness
2. Security
3. Data integrity
4. Architectural integrity
5. Maintainability
6. Performance
7. Testability
8. Minimal and reviewable changes

Do not optimize for speed at the expense of correctness.

---

2. Project Context

Yonte is an Android-only Kotlin application using a modular architecture.

The repository currently contains:

- ":app" — application entry point, dependency wiring, and navigation host
- ":core:database" — Room database, DAOs, repositories, and database encryption integration
- ":core:security" — biometric authentication, key derivation, and encryption management
- ":core:backup" — backup and restore
- ":core:navigation" — navigation definitions
- ":core:designsystem" — shared Compose theme and design tokens
- ":core:update" — update checking and update services
- ":feature:notes" — notes CRUD, editor, search, and ViewModel logic
- ":feature:settings" — settings UI and behavior
- ":feature:onboarding" — first-run onboarding and unlock flow

Treat the actual repository structure and build configuration as authoritative. Do not assume this list is permanently complete.

---

3. Source of Truth

When information conflicts, use this order of authority:

1. Explicit user requirements
2. Security and data-integrity invariants in this file
3. Actual source code and repository state
4. Build configuration and CI configuration
5. Project documentation
6. Currently available Agent Skills
7. General engineering conventions

Never invent repository facts.

If a fact can be inspected, inspect it instead of guessing.

Examples:

- Do not guess a dependency version; inspect the version catalog.
- Do not guess a module dependency; inspect Gradle configuration.
- Do not guess whether a test exists; search the repository.
- Do not claim CI passed without checking CI evidence.
- Do not claim a Skill or Subagent was used unless it was actually used.

---

4. Version and Dependency Policy

This file intentionally does not pin versions of:

- Kotlin
- Gradle
- Android Gradle Plugin
- Android SDK
- Compose
- Room
- Hilt
- SQLCipher
- Any other library
- OpenCode
- Agent Skills
- Agent models

The canonical dependency configuration is maintained by the repository's Gradle configuration and version catalog.

Rules:

- Never duplicate dependency versions in this file.
- Never upgrade or downgrade dependencies as incidental cleanup.
- Do not introduce a dependency when the existing platform or project dependencies already provide the required capability.
- A dependency change must be justified by the task.
- Check compatibility and transitive impact before introducing a dependency.
- Preserve the existing dependency-management strategy.
- Never assume a dependency version from memory.

---

5. Agent Skills

Skills are external capabilities and are intentionally not pinned by name or version in this file.

The currently available Agent Skills are the source of truth.

For each task:

1. Discover the currently available Skills.
2. Identify the Skills relevant to the actual task.
3. Load only the Skills that materially help with the task.
4. Prefer specialized Skills over generic guidance when applicable.
5. Do not assume a Skill exists because it is mentioned in documentation.
6. Do not invent Skill names.
7. Do not claim a Skill was used unless it was actually loaded and applied.
8. Do not duplicate Skill instructions in this file.

Skills provide reusable expertise.

This file provides Yonte-specific constraints.

A Skill must never override a project-specific security, architecture, data-integrity, or scope constraint defined here.

---

6. Subagent Policy

For every non-trivial software task, specialized Subagents are mandatory.

The exact Subagent names are intentionally not specified here.

Discover the available Subagents from the current OpenCode environment and assign them by role.

Required roles for non-trivial changes:

Implementation Role

Responsible for implementing the approved change within the defined scope.

Independent Review Role

Must independently challenge the implementation.

The reviewer must actively search for:

- Correctness defects
- Architecture violations
- Security issues
- Data-loss risks
- Concurrency/lifecycle problems
- Performance regressions
- Edge cases
- Test gaps
- Scope creep
- Unnecessary abstractions
- Regressions

The reviewer must not simply restate the implementer's conclusion.

Verification Role

Responsible for validating the resulting change and examining available verification evidence.

Verification must distinguish:

- Code failures
- Test failures
- Architecture failures
- Static-analysis failures
- CI failures
- Infrastructure failures
- Environment failures

Model Policy

Subagents must use the same current model selection as the primary OpenCode agent.

Do not hard-code a model, provider, model version, reasoning level, or model-specific assumption in this repository.

Model selection belongs to the current OpenCode environment.

Changing the model must not require changing this file.

Subagent Safety

No Subagent may:

- Push code
- Merge code
- Publish a release
- Deploy
- Rewrite Git history
- Perform destructive Git operations
- Change unrelated files

unless the user explicitly authorizes that action.

If a required Subagent role is genuinely unavailable, do not pretend that the role was performed. Report the missing capability and do not claim full verification.

---

7. Task Classification

Not every task requires the full workflow.

Simple tasks

Examples:

- Documentation-only changes
- Typographical fixes
- Small resource changes
- Clearly isolated trivial edits

Use the minimum safe workflow.

Non-trivial tasks

Examples:

- New features
- Multi-file changes
- Architecture changes
- Database changes
- Security changes
- Encryption changes
- Navigation changes
- Large refactors
- Performance-sensitive changes
- CI/CD changes
- Release-related changes

Use the complete Subagent and verification workflow.

When uncertain, classify the task as non-trivial.

---

8. Task Execution Lifecycle

For non-trivial tasks, follow this lifecycle:

Phase 1 — Understand

- Read the relevant project instructions.
- Understand the user's exact requirement.
- Identify explicit constraints.
- Identify what must not change.

Phase 2 — Discover

Inspect the repository before editing.

Determine:

- Relevant modules
- Existing implementations
- Existing patterns
- Dependency relationships
- Tests
- Build configuration
- Security boundaries
- Data flow
- Existing abstractions

Do not create a new abstraction before checking whether an existing one already solves the problem.

Phase 3 — Plan

Create the smallest technically correct implementation plan.

The plan must identify:

- Files/modules likely to change
- Architectural impact
- Data/security impact
- Testing strategy
- Verification strategy
- Risks

Do not expand scope without justification.

Phase 4 — Implement

Implement only the approved scope.

Prefer:

- Existing project patterns
- Existing abstractions
- Small cohesive changes
- Explicit state ownership
- Strong typing
- Clear error propagation

Avoid:

- Parallel abstractions
- Duplicate systems
- Speculative refactoring
- Unnecessary wrappers
- Premature generalization
- Compatibility hacks
- Dead code

Phase 5 — Independent Review

A separate review role must inspect the implementation independently.

All findings must be classified by severity.

Critical and high-severity findings must be resolved before completion.

Phase 6 — Verification

Run or inspect the verification mechanisms permitted by the project.

Do not confuse:

- "I wrote the code"
- "The code looks correct"
- "The code compiled"
- "Tests passed"
- "CI passed"

These are different claims.

Phase 7 — Final Audit

Before declaring completion:

- Review the final diff.
- Confirm no unrelated changes were introduced.
- Confirm architecture boundaries.
- Confirm security invariants.
- Confirm database safety.
- Confirm tests/verification evidence.
- Confirm no secrets or sensitive data were introduced.
- Confirm the requested behavior is actually implemented.
- If the change touches any path under `core/`, `feature/`, `app/`, `.opencode/`, or
  `.github/workflows/`, confirm a `CHANGELOG.md` entry describing the change was added
  in the same commit — `tools/check_changelog.py` enforces this in CI and will fail the
  build otherwise. Add the entry yourself as part of normal task completion; do not
  wait to be asked, and do not treat a changelog-gate failure as a surprise to escalate
  back to the user — it is a routine, expected part of finishing any task in scope.
  Follow the existing format in `CHANGELOG.md` (a dated `## Unreleased — <short
  summary> (YYYY-MM-DD)` section with one or more bullet points, each naming the
  specific file(s)/behavior changed and the relevant commit hash once known).

---

9. Architecture Rules

The project uses feature isolation and core modules.

Dependency direction must remain acyclic and one-way.

Rules:

- Feature modules must not depend on other feature modules.
- Feature modules must not depend on ":app".
- Core modules must not depend on ":app".
- Core modules must not depend on feature modules.
- Feature Gradle files must not declare dependencies on other feature modules.
- Application wiring belongs in ":app".
- Shared functionality belongs in an appropriate core module.
- Feature-specific behavior belongs in its feature module.

Before creating a new module, abstraction, interface, or shared component, verify that an existing module or abstraction cannot reasonably satisfy the requirement.

The repository architecture checker is authoritative when executed in CI.

Never bypass, weaken, or modify an architecture check merely to make a change pass.

---

10. Kotlin and Compose Engineering

Follow the existing Kotlin and Compose architecture.

Rules:

- Use the narrowest visibility that satisfies actual usage.
- Prefer strict typing.
- Avoid "Any", unsafe casts, reflection, and unchecked operations unless clearly justified.
- Keep state ownership explicit.
- Do not perform uncontrolled side effects directly in Composable bodies.
- Use "remember", "derivedStateOf", and effect APIs only when justified by actual state flow.
- Respect lifecycle, coroutine, cancellation, threading, and configuration-change semantics.
- Follow the existing Material 3 and Yonte design-system conventions.
- Do not introduce a parallel design system.
- Do not introduce a second state-management pattern without an explicit architectural reason.

---

11. Security Invariants

Security-sensitive code must preserve the existing security model.

Never:

- Hard-code secrets, credentials, tokens, passwords, signing material, or encryption keys.
- Log passwords, passphrases, keys, tokens, decrypted note content, or other sensitive material.
- Persist plaintext encryption keys.
- Bypass the established key-derivation process.
- Weaken cryptographic parameters to improve performance.
- Replace established cryptographic primitives with custom cryptography.
- Open protected data before the required authentication/unlock state exists.
- Disable authentication or encryption to simplify testing.
- Suppress a security failure merely to make CI pass.

Validate untrusted input at system boundaries.

Security-sensitive changes require focused review and verification.

---

12. Database and Data Integrity

The database is security- and data-critical.

Rules:

- Preserve the existing encrypted-database architecture.
- Never introduce a plaintext fallback for protected application data.
- Every schema change must account for migration compatibility.
- Never use destructive migration as a shortcut for production data.
- Check migration impact before changing entities, indices, queries, or database configuration.
- Add or update migration tests when required by the schema change.
- Do not silently discard user data.
- Do not change persistence semantics as incidental cleanup.

The repository layer remains the appropriate boundary for data-access errors and data-layer behavior.

---

13. Encryption-First Onboarding

The application must not access the protected database before the required unlock/session key is available.

The existing architecture includes:

- User passphrase-based key derivation
- Argon2-based derivation
- Local key/session management
- Encrypted database access
- Lazy repository/database initialization where required

Preserve this invariant:

«First-run or locked application state must never accidentally initialize or access the protected database.»

Any change affecting startup, dependency injection, repositories, authentication, key management, or database initialization must explicitly verify this behavior.

---

14. Performance and Lifecycle

Do not optimize based on speculation.

For performance-sensitive changes:

- Identify the actual bottleneck.
- Prefer measurement or evidence where practical.
- Avoid unnecessary recomposition.
- Avoid unnecessary allocations.
- Avoid excessive database writes.
- Respect coroutine cancellation.
- Avoid blocking the main thread.
- Avoid memory leaks.
- Avoid long-lived references to lifecycle-bound objects.

Do not sacrifice correctness or security for micro-optimizations.

---

15. Testing Policy

Tests must validate behavior, not implementation trivia.

For changes affecting behavior:

- Identify the critical paths.
- Preserve existing tests.
- Add focused tests for new behavior.
- Include regression coverage for fixed defects when practical.
- Test security- and data-critical paths explicitly.

Never:

- Delete a failing test merely because it fails.
- Weaken assertions to hide a regression.
- Skip a relevant test without documenting why.
- Modify production behavior solely to satisfy an incorrectly designed test.

If verification cannot be performed in the current environment, state that explicitly.

---

16. Failure Handling

When verification fails:

1. Stop and classify the failure.
2. Determine the root cause.
3. Inspect the evidence.
4. Fix the actual cause when it is within scope.
5. Re-run the relevant verification.
6. Do not blindly retry.
7. Do not modify unrelated code to make the check pass.
8. Do not suppress errors without a justified reason.

Never "fix" verification by:

- Disabling the test
- Weakening assertions
- Suppressing warnings without justification
- Bypassing architecture checks
- Removing validation
- Disabling security controls
- Changing CI behavior solely to hide a failure

After repeated CI failures for the same task, stop rather than entering an uncontrolled retry loop and report the failure with evidence.

---

17. Scope and Change Control

Modify only what the task requires.

Do not:

- Perform unrelated cleanup.
- Rename unrelated files.
- Reformat unrelated code.
- Upgrade dependencies incidentally.
- Introduce unrelated architectural improvements.
- Rewrite working systems merely because a different pattern is preferred.

Preserve existing behavior unless a behavior change is explicitly requested.

Ask for user approval before:

- Materially expanding scope
- Changing architecture
- Introducing a new dependency
- Changing public behavior
- Performing destructive operations
- Changing release/signing behavior
- Changing security guarantees
- Changing persistent data semantics

---

18. Git Policy

Git history is user-controlled.

Never automatically:

- Push
- Merge
- Create a release
- Tag a release
- Rewrite history
- Reset destructively
- Force-push
- Delete branches
- Create commits merely because implementation is complete

Only create commits when explicitly requested.

Keep requested commits focused and reviewable.

Before destructive Git operations, obtain explicit user approval.

---

19. CI and Build Policy

GitHub Actions is the authoritative build, test, lint, and release-verification environment for this repository.

Do not treat a local Android/Gradle build as equivalent to the project's canonical CI verification.

The existing GitHub Actions workflow is the source of truth for:

- Java/runtime setup
- Android SDK setup
- Architecture checks
- Compilation
- Tests
- Lint
- Debug APK artifacts
- Signed release builds
- APK signature verification

Do not duplicate CI version/configuration values in this file.

If CI configuration changes, inspect the actual workflow rather than relying on this document.

---

20. APK and Release Policy

Release APKs are produced through GitHub Actions.

The canonical release flow is:

Repository
    ↓
GitHub
    ↓
GitHub Actions
    ↓
Build + Tests + Lint + Verification
    ↓
Release Build
    ↓
Signature Verification
    ↓
GitHub Artifact / Release workflow

Do not treat a locally generated APK as the authoritative release artifact.

Do not:

- Commit generated APKs unless explicitly required.
- Expose signing keys or credentials.
- Move signing credentials into source control.
- Bypass CI signing controls.
- Claim a release APK exists without actual GitHub Actions evidence.

Release publication still requires explicit user authorization.

---

21. Evidence Before Claims

Never claim:

- "Build passed"
- "Tests passed"
- "CI passed"
- "APK generated"
- "APK signed"
- "Architecture is valid"
- "Security was verified"

without actual evidence.

If something was not executed or could not be verified, say so.

Use precise language:

- Verified
- Not verified
- Blocked
- Failed
- Environment failure
- Requires user action

Never convert uncertainty into a success claim.

---

22. Anti-Overengineering Rules

Prefer the simplest solution that correctly satisfies the requirement.

Do not introduce:

- New layers without a concrete need
- New managers for existing responsibilities
- Duplicate repositories
- Duplicate state holders
- Duplicate navigation systems
- Duplicate design systems
- Speculative abstractions
- Generic frameworks for one-off behavior

Before adding an abstraction, answer:

1. What concrete problem does it solve?
2. Why can the existing abstraction not solve it?
3. Does it reduce complexity or merely move it?
4. Will it remain useful after this task?

If these questions cannot be answered clearly, do not add the abstraction.

---

23. Completion Contract

A task is complete only when the implementation and verification state are clear.

The final report for non-trivial work must contain:

Changed

What was changed and why.

Scope

Which modules/files were affected.

Architecture

Whether dependency boundaries or architectural behavior changed.

Security

Whether security-sensitive code or data flow changed.

Verification

Exactly what was verified and where.

CI

Relevant GitHub Actions result/evidence, when applicable.

Tests

Tests executed, tests not executed, and reasons.

Risks

Known limitations, unresolved issues, or environment blockers.

Never finish with a generic "done" when meaningful verification information is available.

---

24. Final Rule

The agent must behave as an engineering system, not as an autocomplete engine.

Before changing code:

«Inspect → Understand → Plan → Implement → Independently Review → Verify → Audit.»

When uncertain:

«Inspect before assuming.»

When a shortcut conflicts with correctness:

«Choose correctness.»

When a Skill conflicts with a project invariant:

«Preserve the project invariant.»

When verification fails:

«Find the root cause instead of hiding the failure.»

When the task is complete:

«Provide evidence, not confidence.»
