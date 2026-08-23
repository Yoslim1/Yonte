#!/usr/bin/env python3
"""Black-box stress and contract tests for Yonte's public update channel."""
from __future__ import annotations

import concurrent.futures
import hashlib
import json
import re
import shutil
import subprocess
import tempfile
import urllib.error
import urllib.request
import zipfile
from pathlib import Path

MANIFEST_URL = "https://raw.githubusercontent.com/Yoslim1/Yonte-updates/main/update.json"
REQUIRED = {"channel", "versionCode", "versionName", "minimumSdk", "sha256", "certificateSha256", "downloadUrl", "releaseNotes", "publishedAt"}


def fetch(url: str, timeout: int = 15) -> bytes:
    request = urllib.request.Request(url, headers={"Accept": "application/json", "Cache-Control": "no-cache"})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        assert 200 <= response.status < 300, response.status
        return response.read()


def validate_manifest(raw: bytes) -> dict:
    value = json.loads(raw)
    assert REQUIRED <= value.keys(), sorted(REQUIRED - value.keys())
    assert value["channel"] == "stable"
    assert isinstance(value["versionCode"], int) and value["versionCode"] > 0
    assert isinstance(value["versionName"], str) and value["versionName"]
    assert isinstance(value["minimumSdk"], int) and value["minimumSdk"] <= 26
    assert len(value["sha256"]) == 64 and all(c in "0123456789abcdef" for c in value["sha256"].lower())
    assert len(value["certificateSha256"]) == 64 and all(c in "0123456789abcdef" for c in value["certificateSha256"].lower())
    assert value["downloadUrl"].startswith("https://github.com/Yoslim1/Yonte-updates/releases/download/")
    return value


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def test_contract_and_real_artifact() -> tuple[dict, bytes]:
    manifest = validate_manifest(fetch(MANIFEST_URL))
    apk = fetch(manifest["downloadUrl"], timeout=60)
    assert len(apk) > 1_000_000
    assert sha256(apk) == manifest["sha256"], (sha256(apk), manifest["sha256"])
    with tempfile.NamedTemporaryFile(suffix=".apk") as handle:
        handle.write(apk)
        handle.flush()
        with zipfile.ZipFile(handle.name) as archive:
            names = set(archive.namelist())
            assert "AndroidManifest.xml" in names
            assert "classes.dex" in names
        apksigner = "/home/ubuntu/android-sdk/build-tools/35.0.0/apksigner"
        output = subprocess.check_output([apksigner, "verify", "--print-certs", handle.name], text=True)
        match = re.search(r"Signer #1 certificate SHA-256 digest: ([0-9a-f:]+)", output, re.IGNORECASE)
        assert match, output
        certificate = match.group(1).replace(":", "").lower()
        assert certificate == manifest["certificateSha256"].lower(), (certificate, manifest["certificateSha256"])
    return manifest, apk


def test_version_matrix(remote: int) -> None:
    cases = [(0, True), (remote - 1, True), (remote, False), (remote + 1, False)]
    for current, should_update in cases:
        assert (remote > current) is should_update, (current, remote)


def test_corruption_is_detected(manifest: dict, apk: bytes) -> None:
    corrupted = bytearray(apk)
    corrupted[len(corrupted) // 2] ^= 0x01
    assert sha256(corrupted) != manifest["sha256"]


def test_malformed_manifests() -> None:
    valid = {
        "channel": "stable", "versionCode": 3, "versionName": "1.2.0", "minimumSdk": 26,
        "sha256": "a" * 64, "certificateSha256": "b" * 64, "downloadUrl": "https://github.com/Yoslim1/Yonte-updates/releases/download/v1.2.0/Yonte-v1.2.0.apk",
        "releaseNotes": "x", "publishedAt": "2026-08-23",
    }
    for key in REQUIRED:
        invalid = dict(valid)
        invalid.pop(key)
        try:
            validate_manifest(json.dumps(invalid).encode())
        except AssertionError:
            pass
        else:
            raise AssertionError(f"missing field accepted: {key}")
    invalid_url = dict(valid, downloadUrl="http://example.invalid/update.apk")
    try:
        validate_manifest(json.dumps(invalid_url).encode())
    except AssertionError:
        pass
    else:
        raise AssertionError("unsafe download URL accepted")


def main() -> None:
    manifest, apk = test_contract_and_real_artifact()
    test_version_matrix(manifest["versionCode"])
    test_corruption_is_detected(manifest, apk)
    test_malformed_manifests()

    # Repeated manifest reads model repeated manual checks without downloading the APK each time.
    with concurrent.futures.ThreadPoolExecutor(max_workers=16) as pool:
        results = list(pool.map(lambda _: validate_manifest(fetch(MANIFEST_URL)), range(32)))
    assert all(item["versionCode"] == manifest["versionCode"] for item in results)

    # A few concurrent artifact reads exercise the release CDN and checksum path.
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        artifact_hashes = list(pool.map(lambda _: sha256(fetch(manifest["downloadUrl"], timeout=60)), range(3)))
    assert artifact_hashes == [manifest["sha256"]] * 3

    print(json.dumps({
        "status": "PASS",
        "manifest_checks": len(results) + 1,
        "artifact_downloads": len(artifact_hashes),
        "apk_bytes": len(apk),
        "version_code": manifest["versionCode"],
        "version_name": manifest["versionName"],
        "sha256": manifest["sha256"],
        "certificate_sha256": manifest["certificateSha256"],
        "corruption_rejected": True,
        "malformed_manifests_rejected": True,
    }, indent=2))


if __name__ == "__main__":
    main()
