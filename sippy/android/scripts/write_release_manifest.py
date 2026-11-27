#!/usr/bin/env python3
"""Write releases/<tag>.json and releases/latest.json for a published APK.

Called by .github/workflows/release.yml after `gh release create` succeeded.
The manifest is the contract the in-app updater reads:
  versionCode, versionName, apkUrl, sha256, sizeBytes, minSdk, releasedAt, notes
Unknown fields are ignored by older builds; every field has a default.
"""

from __future__ import annotations

import argparse
import datetime
import json
import pathlib

REPOSITORY = "Cooreo/SippyDippy"


def build_manifest(tag: str, name: str, code: int, sha: str, size: int, apk: str) -> dict:
    return {
        "versionCode": code,
        "versionName": name,
        "apkUrl": f"https://github.com/{REPOSITORY}/releases/download/{tag}/{apk}",
        "sha256": sha,
        "sizeBytes": size,
        "minSdk": 26,
        "releasedAt": datetime.datetime.now(datetime.timezone.utc).strftime(
            "%Y-%m-%dT%H:%M:%SZ"
        ),
        "notes": [f"Sippy {name}", "Signed release built by GitHub Actions"],
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dir", default=".", help="repository checkout to write into")
    parser.add_argument("--tag", required=True)
    parser.add_argument("--name", required=True)
    parser.add_argument("--code", type=int, required=True)
    parser.add_argument("--sha", required=True)
    parser.add_argument("--size", type=int, required=True)
    parser.add_argument("--apk", required=True)
    args = parser.parse_args()

    manifest = build_manifest(
        args.tag, args.name, args.code, args.sha, args.size, args.apk
    )
    root = pathlib.Path(args.dir) / "releases"
    root.mkdir(parents=True, exist_ok=True)
    text = json.dumps(manifest, indent=2) + "\n"
    (root / f"{args.tag}.json").write_text(text)
    (root / "latest.json").write_text(text)
    print(text, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
