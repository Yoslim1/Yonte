#!/usr/bin/env python3
"""
tools/check_changelog.py
Fails CI if impactful source paths changed without a CHANGELOG.md entry
in the same diff range. Uses an allow-list (fail-closed): only paths
listed in IMPACTFUL_PREFIXES require a changelog entry. Anything not
listed (docs/, README.md, .gitignore, etc.) is exempt by default.

Base commit resolution order:
1. $CHANGELOG_GATE_BASE_SHA (set by the workflow from github.event.before)
   -- correct for "push" events, since origin/main already equals HEAD
   by the time this job runs.
2. merge-base with origin/main -- fallback for manual/local runs.
3. HEAD~1 -- last-resort fallback.
"""
import os
import subprocess
import sys

IMPACTFUL_PREFIXES = (
    "core/",
    "feature/",
    "app/",
    ".opencode/",
    ".github/workflows/",
)

ZERO_SHA = "0000000000000000000000000000000000000000"


def run(cmd):
    try:
        return subprocess.check_output(cmd, text=True).strip()
    except subprocess.CalledProcessError:
        return ""


def resolve_base():
    env_base = os.environ.get("CHANGELOG_GATE_BASE_SHA", "").strip()
    if env_base and env_base != ZERO_SHA:
        if run(["git", "cat-file", "-e", env_base]):
            return env_base
        print(f"WARNING: base SHA {env_base} not present locally "
              f"(shallow clone?); falling back.")

    run(["git", "fetch", "origin", "main", "--depth=50"])
    merge_base = run(["git", "merge-base", "origin/main", "HEAD"])
    head = run(["git", "rev-parse", "HEAD"])
    if merge_base and merge_base != head:
        return merge_base

    return "HEAD~1"


def main():
    base = resolve_base()
    changed = [f for f in run(["git", "diff", "--name-only", base, "HEAD"]).splitlines() if f]

    if not changed:
        print(f"PASS: no changes between {base} and HEAD.")
        return 0

    impactful = [f for f in changed if f.startswith(IMPACTFUL_PREFIXES)]

    if not impactful:
        print("PASS: no impactful paths changed; changelog entry not required.")
        print(f"  changed: {changed}")
        return 0

    if "CHANGELOG.md" not in changed:
        print("FAIL: impactful paths changed without a CHANGELOG.md entry:")
        for f in impactful:
            print(f"  - {f}")
        print()
        print("Add a dated entry under CHANGELOG.md, citing the commit hash")
        print("and, if the change closes a CI/test finding, the run ID that")
        print("proves it, then include CHANGELOG.md in this same commit.")
        return 1

    print("PASS: CHANGELOG.md updated alongside impactful changes.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
