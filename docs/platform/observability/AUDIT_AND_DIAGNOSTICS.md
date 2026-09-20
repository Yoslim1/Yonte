# Audit and Diagnostics

## Purpose

Make Yonte diagnosable without becoming a secondary repository of user content.

## Allowed metadata

- correlation/operation ID.
- actor type.
- capability/action.
- entity ID/type (when safe/needed).
- result/error code.
- timing/duration.
- policy/contract version.

## Forbidden

- note/task/file bodies.
- passphrases/PINs/keys/recovery secrets.
- decrypted backup content.
- raw sensitive AI prompts.

## Rules

One user operation may propagate a correlation ID across AI request, command, database transaction, event, and error handling so engineers can trace the flow without content logging.
