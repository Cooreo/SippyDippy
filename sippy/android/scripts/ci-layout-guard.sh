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
import glob, xml.etree.ElementTree as ET

reports = sorted(glob.glob("app/build/outputs/androidTest-results/connected/**/*.xml", recursive=True))
if not reports:
    lines = [l.strip() for l in open("/tmp/guard.log").read().splitlines() if l.strip()]
    print("::error::No androidTest XML; log tail: " + " | ".join(lines[-15:])[:450])
else:
    for path in reports:
        root = ET.parse(path).getroot()
        tests = root.get("tests", "?")
        failures = root.get("failures", "0")
        errors = root.get("errors", "0")
        print(f"::warning::Report {path}: tests={tests} failures={failures} errors={errors}")
        for tc in root.findall("testcase"):
            fail = tc.find("failure")
            if fail is None:
                fail = tc.find("error")
            if fail is not None:
                msg = (fail.get("message") or fail.text or "").strip().replace("\n", " ")[:450]
                print(f"::error::{tc.get('classname')}.{tc.get('name')}: {msg}")
PY
  exit "$status"
fi

echo "::warning::LayoutGuardTest passed cleanly"
