#!/usr/bin/env bash
set -uo pipefail

cd "$(dirname "$0")/.."
echo "::warning::Running connectedDebugAndroidTest for LayoutGuardTest"

./gradlew --no-daemon --stacktrace connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.cooreo.sippy.LayoutGuardTest \
  > /tmp/guard.log 2>&1
status=$?

cat /tmp/guard.log

if [ "$status" -ne 0 ]; then
  python3 - <<'PY'
import glob, re, xml.etree.ElementTree as ET

reports = sorted(glob.glob("app/build/outputs/androidTest-results/connected/**/*.xml", recursive=True))
if not reports:
    lines = [l.strip() for l in open("/tmp/guard.log").read().splitlines() if l.strip()]
    print("::error::No androidTest XML; log tail: " + " | ".join(lines[-15:])[:600])
else:
    for path in reports:
        raw = open(path).read()
        root = ET.fromstring(raw)
        emitted = 0
        for elem in root.iter():
            tag = elem.tag.split("}")[-1]
            if tag in ("failure", "error"):
                msg = re.sub(r'\s+', ' ', (elem.get("message") or elem.text or "")).strip()
                print(f"::error::{msg[:500]}")
                emitted += 1
        if emitted == 0:
            compact = re.sub(r'\s+', ' ', raw).strip()
            print(f"::error::XML dump ({path}): {compact[:700]}")
PY
  exit "$status"
fi

echo "::warning::LayoutGuardTest passed cleanly"
