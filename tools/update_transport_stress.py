#!/usr/bin/env python3
"""Transport-level stress tests for the update channel contract."""
from __future__ import annotations

import hashlib
import json
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.error import HTTPError
from urllib.request import urlopen

APK = b"fake-apk-payload" * 100
GOOD = json.dumps({
    "channel": "stable", "versionCode": 3, "versionName": "1.2.0", "minimumSdk": 26,
    "sha256": hashlib.sha256(APK).hexdigest(),
    "downloadUrl": "https://github.com/Yoslim1/Yonte-updates/releases/download/v1.2.0/Yonte-v1.2.0.apk",
    "releaseNotes": "test", "publishedAt": "2026-08-23",
}).encode()


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):  # noqa: N802
        if self.path == "/good.json":
            body, status = GOOD, 200
        elif self.path == "/bad.json":
            body, status = b"{not-json", 200
        elif self.path == "/server-error":
            body, status = b"error", 503
        elif self.path == "/corrupt.apk":
            body, status = APK[:-1] + b"X", 200
        elif self.path == "/slow":
            time.sleep(0.15)
            body, status = GOOD, 200
        else:
            body, status = b"missing", 404
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        try:
            self.wfile.write(body)
        except BrokenPipeError:
            pass

    def log_message(self, *_args):
        return


def get(url: str, timeout: float = 1.0) -> bytes:
    with urlopen(url, timeout=timeout) as response:
        assert response.status == 200
        return response.read()


def main() -> None:
    server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    base = f"http://127.0.0.1:{server.server_port}"
    try:
        assert json.loads(get(base + "/good.json"))["versionCode"] == 3
        for _ in range(100):
            assert json.loads(get(base + "/good.json"))["sha256"] == hashlib.sha256(APK).hexdigest()
        try:
            get(base + "/server-error")
        except HTTPError as exc:
            assert exc.code == 503
        else:
            raise AssertionError("HTTP 503 was accepted")
        try:
            json.loads(get(base + "/bad.json"))
        except json.JSONDecodeError:
            pass
        else:
            raise AssertionError("malformed JSON was accepted")
        assert hashlib.sha256(get(base + "/corrupt.apk")).hexdigest() != hashlib.sha256(APK).hexdigest()
        start = time.monotonic()
        try:
            get(base + "/slow", timeout=0.05)
        except TimeoutError:
            pass
        except Exception as exc:  # urllib raises a socket timeout wrapped by the platform.
            assert "timed out" in str(exc).lower() or "timeout" in str(exc).lower()
        else:
            raise AssertionError("slow response was not bounded by timeout")
        print(json.dumps({"status": "PASS", "repeated_reads": 100, "http_503_rejected": True, "malformed_json_rejected": True, "corrupt_payload_detected": True, "timeout_bounded": True}, indent=2))
    finally:
        server.shutdown()
        server.server_close()


if __name__ == "__main__":
    main()
