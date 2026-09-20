# Secrets and Configuration Policy

## Purpose

Keep credentials, provider secrets, signing material, and environment-specific configuration out of source code and untrusted runtime paths.

## Rules

- Never commit API keys, passwords, signing keys, recovery secrets, or private credentials.
- Never place secrets in logs, analytics, crash metadata, events, or screenshots.
- Build-time configuration must distinguish public configuration from secrets.
- User/provider credentials stored on device use the appropriate secure storage/key lifecycle boundary.
- Remote configuration may tune non-security behavior but cannot silently weaken security invariants, permissions, crypto policy, or trusted signer rules.
- Development/test credentials are isolated from production and must not be accepted by production builds.
- Secret rotation and revocation must have an explicit recovery/failure path.

## Review

Any new external provider must document what secret exists, who owns it, where it lives, how it rotates, and what happens when it is unavailable.
