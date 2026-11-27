#!/usr/bin/env python3
"""Write releases/<tag>.json and releases/latest.json for a published APK.

Called by .github/workflows/release.yml after `gh release create` succeeded.
The manifest is the contract the in-app updater reads:
  versionCode, versionName, apkUrl, sha256, sizeBytes, minSdk, releasedAt, notes,
  certSha256
`certSha256` is written only by a real publish (this script only runs in the
tags-only publish job). CI compares the built APK's certificate with it before
anything is released, so a second key can never ship — trust on first use.
Unknown fields are ignored by older builds; every field has a default.
"""

from __future__ import annotations

import argparse
import datetime
import json
import pathlib

REPOSITORY = "Cooreo/SippyDippy"


def normalise_cert(value: str) -> str:
    """Lowercase hex, no colons — the form apksigner and CI use."""
    return "".join(c for c in value.lower() if c in "0123456789abcdef")


def build_manifest(
    tag: str, name: str, code: int, sha: str, size: int, apk: str, cert: str = ""
) -> dict:
    manifest = {
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
    cert = normalise_cert(cert)
    if cert:
        manifest["certSha256"] = cert
    return manifest


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dir", default=".", help="repository checkout to write into")
    parser.add_argument("--tag", required=True)
    parser.add_argument("--name", required=True)
    parser.add_argument("--code", type=int, required=True)
    parser.add_argument("--sha", required=True)
    parser.add_argument("--size", type=int, required=True)
    parser.add_argument("--apk", required=True)
    parser.add_argument("--cert-sha256", default="")
    args = parser.parse_args()

    manifest = build_manifest(
        args.tag, args.name, args.code, args.sha, args.size, args.apk, args.cert_sha256
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
