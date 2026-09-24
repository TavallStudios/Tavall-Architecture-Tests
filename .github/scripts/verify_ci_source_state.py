#!/usr/bin/env python3
"""Verify that Tavall CI is executing one clean, exact Git source state."""
from __future__ import annotations

import argparse
import subprocess
import sys
import tempfile
from pathlib import Path

HEAD_MISMATCH_EXIT = 65
DIRTY_SOURCE_EXIT = 66


def git(root: Path, *arguments: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["git", "-C", str(root), *arguments],
        check=True,
        capture_output=True,
        text=True,
    )


def verify(root: Path, expected_head: str) -> int:
    actual_head = git(root, "rev-parse", "HEAD").stdout.strip()
    if expected_head and actual_head != expected_head:
        print(
            f"CI source HEAD mismatch: expected={expected_head} actual={actual_head}",
            file=sys.stderr,
        )
        return HEAD_MISMATCH_EXIT

    status = git(root, "status", "--porcelain=v1", "--untracked-files=all").stdout
    if status.strip():
        print("CI source worktree is dirty; exact-head evidence is invalid:", file=sys.stderr)
        for line in status.rstrip().splitlines():
            print(f"  {line}", file=sys.stderr)
        return DIRTY_SOURCE_EXIT
    return 0


def initialize_fixture(root: Path) -> str:
    git(root, "init", "-q")
    git(root, "config", "user.name", "Tavall CI Fixture")
    git(root, "config", "user.email", "ci-fixture@tavall.invalid")
    (root / "tracked.txt").write_text("clean\n", encoding="utf-8")
    git(root, "add", "tracked.txt")
    git(root, "commit", "-q", "-m", "fixture")
    return git(root, "rev-parse", "HEAD").stdout.strip()


def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="tavall-ci-source-") as directory:
        root = Path(directory)
        head = initialize_fixture(root)
        if verify(root, head) != 0:
            raise AssertionError("clean exact-head fixture was rejected")

        wrong_head = "0" * len(head)
        if wrong_head == head:
            wrong_head = "1" * len(head)
        if verify(root, wrong_head) != HEAD_MISMATCH_EXIT:
            raise AssertionError("moved-head fixture was not rejected")

        (root / "tracked.txt").write_text("dirty\n", encoding="utf-8")
        if verify(root, head) != DIRTY_SOURCE_EXIT:
            raise AssertionError("dirty tracked fixture was not rejected")
        git(root, "checkout", "--", "tracked.txt")

        (root / "untracked.txt").write_text("new source\n", encoding="utf-8")
        if verify(root, head) != DIRTY_SOURCE_EXIT:
            raise AssertionError("untracked fixture was not rejected")

    print("CI exact-source guard self-test passed.")


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify exact Tavall CI Git source state")
    parser.add_argument("--root", default=".")
    parser.add_argument("--expected-head", default="")
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args()

    if arguments.self_test:
        self_test()
        return 0
    return verify(Path(arguments.root).resolve(), arguments.expected_head)


if __name__ == "__main__":
    raise SystemExit(main())
