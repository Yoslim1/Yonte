# Security Foundation Contract

## Purpose
Define the security foundation before building future features.

## Rules

- Identity is the root of access decisions.
- Features never access other feature data directly.
- Permissions are capability based.
- Secrets must have a defined lifecycle.
- Sensitive operations require explicit authorization.
- Destructive operations require user confirmation.
- Security changes must not require feature rewrites.

## Future Domains

Notes, Tasks, AI, Files and other domains consume security contracts; they do not implement their own security rules.

## Principles

- Least privilege.
- Auditability.
- Recoverability.
- Separation of concerns.
- Backward compatibility.
