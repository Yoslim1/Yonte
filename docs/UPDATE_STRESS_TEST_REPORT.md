# Yonte Update System — Stress Test Report

## Scope

This test covers the public update manifest, version comparison, trusted URL validation, HTTPS transport, APK download, SHA-256 verification, signing certificate pinning, malformed input handling, bounded timeouts, and release build integrity.

## Executed checks

| Area | Test | Result |
|---|---|---|
| Manifest contract | Required fields, stable channel, positive version, minimum SDK, SHA-256, certificate digest, trusted release URL | PASS |
| Version logic | Older, equal, and newer local versions | PASS |
| Manifest load | 32 parallel reads plus one baseline read | PASS |
| APK CDN | Three parallel downloads of the published APK | PASS |
| APK integrity | ZIP structure, AndroidManifest.xml, classes.dex, SHA-256 | PASS |
| APK signing | APK signature scheme and certificate SHA-256 match | PASS |
| Corruption | Single-byte payload mutation rejected by checksum mismatch | PASS |
| Malformed data | Missing fields, invalid checksum, unsafe host/path, and HTTP error | PASS |
| Transport failure | HTTP 503, malformed JSON, slow response timeout, corrupt payload | PASS |
| Android build | `:core:update:test`, `:core:database:test`, and `:app:assembleRelease` | PASS |

## Issues found and fixed

The first review identified a serious release-continuity risk: debug APKs built by different CI runners can be signed with different debug keys, causing Android to reject an update over an installed build. Yonte now has a stable release-signing configuration, a documented CI path for repository secrets, and certificate pinning in the update manifest. A signed release artifact was rebuilt and published.

The first version of the stress fixture did not include the new certificate field, so the malformed-manifest loop raised a fixture error. The fixture was corrected and the complete suite was rerun successfully. The local timeout simulation also produced an expected BrokenPipe log when the client disconnected; the test server now suppresses that expected noise.

## Current release evidence

The published manifest advertises version code 3 and version name 1.2.0. The signed APK is 12,154,304 bytes, has SHA-256 `5f68d1c85d21566f5476f962a4ca86eb5cff93f152bed30052593a52f100bfb5`, and has signing-certificate SHA-256 `3679ef199cd7c2471a8ccd128bbc428ff12575b74c1fa4d222760692f2ddab23`.

## Limitation

No Android device or emulator with `adb` was available in the build environment. Therefore, the installer handoff, Android package-manager upgrade over an already-installed APK, and UI behavior under real lifecycle interruptions still require a device or emulator run. The APK signature was verified statically with `apksigner`, and all network, manifest, checksum, and build checks passed.

## Reproducible commands

```bash
python3 tools/update_stress_test.py
python3 tools/update_transport_stress.py
./gradlew :core:update:test :core:database:test :app:assembleRelease
```
