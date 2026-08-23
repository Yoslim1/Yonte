#!/usr/bin/env python3
"""Static architecture guardrails for Yonte's feature-isolation blueprint."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FEATURES = ROOT / "feature"
CORE = ROOT / "core"


def source_files(folder: Path):
    return folder.rglob("*.kt") if folder.exists() else []


def main() -> None:
    violations: list[str] = []
    for path in source_files(FEATURES):
        text = path.read_text(encoding="utf-8")
        if re.search(r"^import com\.yonte\.feature\.", text, re.MULTILINE):
            violations.append(f"feature-to-feature import: {path}")
        if re.search(r"^import com\.yonte\.app\.", text, re.MULTILINE):
            violations.append(f"feature-to-app import: {path}")
    for path in source_files(CORE):
        text = path.read_text(encoding="utf-8")
        if re.search(r"^import com\.yonte\.app\.", text, re.MULTILINE):
            violations.append(f"core-to-app import: {path}")
        if re.search(r"^import com\.yonte\.feature\.", text, re.MULTILINE):
            violations.append(f"core-to-feature import: {path}")
    for path in FEATURES.glob("*/build.gradle.kts"):
        text = path.read_text(encoding="utf-8")
        if re.search(r'project\(":feature:', text):
            violations.append(f"feature-to-feature Gradle dependency: {path}")
    if violations:
        raise SystemExit("\n".join(violations))
    print("ARCHITECTURE PASS: no feature-to-feature or feature/core-to-app edges found")


if __name__ == "__main__":
    main()
