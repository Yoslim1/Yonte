# ADR-009: Update Trust Anchor and APK Identity

## Status

Accepted target architecture.

## Context

Yonte's update flow validates HTTPS release location, downloaded SHA-256, and a signing-certificate SHA-256 supplied by the remote update manifest. The checksum and advertised signer therefore share one mutable remote trust source.

If that metadata source is compromised, remote hashes cannot independently establish which signer or package identity Yonte should trust.

## Decision

Use the **currently installed Yonte package identity and signing certificate lineage** as the local trust anchor for downloaded update APKs.

The remote manifest remains discovery/integrity metadata; it does not grant signer trust.

Before installer handoff, a candidate APK must satisfy all applicable checks:

- package name equals the installed Yonte package name.
- actual candidate version code matches the advertised version and is newer than the installed version.
- actual minimum SDK is compatible and does not silently contradict accepted metadata.
- downloaded bytes match the advertised SHA-256.
- signer identity is trusted by the local policy below.

### Signer policy

On API 28+ use Android `SigningInfo`:

- multi-signer package identity is the complete current signer set; require exact set equality unless a later ADR defines a reviewed multi-signer rotation policy.
- for the normal single-signer case, accept the same current signer.
- if the candidate uses a different current signer, accept only when the candidate's platform-verified signing-certificate history contains the currently installed signer, proving a forward signing-key rotation lineage.
- do not accept a candidate merely because its signer appears in the installed app's past history; the candidate must prove authorization from the currently trusted lineage.

On API 26–27, where the modern rotation proof API is unavailable, require candidate and installed signer sets to match exactly.

The remote `certificateSha256` field may remain as a metadata/file consistency check, but it cannot override this local signer policy.

## Implementation boundary

Separate framework inspection from the trust decision:

```text
AndroidApkIdentityInspector -> PackageManager / archive parsing
UpdateTrustPolicy           -> pure package/version/signer decision
UpdateService               -> download/hash/orchestration/install handoff
```

Remote `versionName` is display metadata and must not control filesystem paths. Candidate cache filenames use locally generated validated data such as numeric version code.

Rejected/unverified partial APKs are removed from cache before the operation completes.

## Consequences

- compromise of mutable update metadata alone cannot redefine Yonte's trusted signer.
- package-confusion updates are rejected before installer handoff.
- signing-key rotation remains possible on modern Android when the candidate proves a valid forward lineage.
- API 26–27 intentionally require a stable signer unless a separately reviewed compatibility mechanism is introduced.
- trust-policy decisions become testable independently from network and PackageManager glue.

## Verification

Focused negative tests cover wrong package, metadata/version mismatch, unrelated signer, malicious manifest-advertised signer, invalid rotation history, multi-signer mismatch, legacy-API signer change, and remote filename manipulation.

Valid same-signer and platform-verified forward-rotation cases remain accepted. Canonical CI and release-signature verification remain required.

## Rejected

- remote manifest certificate fingerprint as the signer authority.
- trusting only the first certificate of a multi-signer APK.
- disabling package/signature validation because Android's installer also performs signing checks.
- adding signed update metadata as a substitute for APK package/signer verification.
