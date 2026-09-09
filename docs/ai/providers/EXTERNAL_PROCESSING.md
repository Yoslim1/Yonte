# External AI Processing

## Purpose

Define privacy rules when AI computation leaves the device.

## Rules

- The UI distinguishes on-device vs network/external processing before sensitive disclosure.
- Send the minimum content required for the user-requested purpose.
- Provider adapters receive scoped context, not unrestricted database access.
- Sensitive content is not retained in Yonte AI memory merely because it was sent for one request.
- Do not promise deletion from provider infrastructure unless technically/contractually verified.
- API keys/provider credentials are secrets and never stored in repository source or logs.

## Future requirement

Provider choice must remain behind a stable interface so local/cloud/model changes do not leak into feature implementations.
