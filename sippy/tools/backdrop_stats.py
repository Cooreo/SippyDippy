#!/usr/bin/env python3
"""Measure the backdrop with the dark-theme scrim applied (round 9, 1.1).

The contrast rules in the app are only honest if the numbers they are checked
against come from the real art. This prints, as JSON:

  mean          the mean colour of the backdrop after the scrim
  meanLuminance relative luminance of that mean
  brightest5    the brightest colours covering 5% of the pixels, after the scrim

ImageMagick does the pixel work (`convert`); the repository has no Python image
dependencies and this is not worth adding one for.
"""
import json
import shutil
import subprocess
import sys
from pathlib import Path

ASSET = str(Path(__file__).resolve().parent.parent / "android/app/src/main/res/drawable-nodpi/backdrop.webp")
# The scrim's own stops, as drawn by ThemeScrim in BackdropHost.
SCRIM_TOP = "rgba(11,31,56,0.35)"
SCRIM_BOTTOM = "rgba(11,31,56,0.75)"
SAMPLE = "270x600"


def convert(*args):
    return subprocess.run(["convert", *args], capture_output=True, text=True, check=True).stdout


def scrimmed(path):
    convert(ASSET, "-resize", SAMPLE, "/tmp/_bd_base.png")
    convert(
        "/tmp/_bd_base.png",
        "(", "-size", SAMPLE, f"gradient:{SCRIM_TOP}-{SCRIM_BOTTOM}", ")",
        "-compose", "over", "-composite", path,
    )


def to_hex(pixel):
    return "#%02X%02X%02X" % tuple(round(c) for c in pixel)


def mean_colour(path):
    out = convert(path, "-resize", "1x1", "txt:-")
    rgb = out.split("(")[1].split(")")[0].split(",")
    return [float(v) for v in rgb[:3]]


def luminance(pixel):
    def channel(value):
        v = value / 255.0
        return v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4

    r, g, b = (channel(c) for c in pixel)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def brightest(path, coverage=0.05):
    out = convert(path, "-colors", "16", "-format", "%c", "histogram:info:-")
    bins = []
    for line in out.splitlines():
        line = line.strip()
        if not line or ":" not in line:
            continue
        count = int(line.split(":")[0])
        if "(" not in line:
            continue
        rgb = line.split("(")[1].split(")")[0].split(",")
        bins.append((luminance([float(v) for v in rgb[:3]]), count, [float(v) for v in rgb[:3]]))
    bins.sort(key=lambda b: b[0], reverse=True)
    total = sum(b[1] for b in bins) or 1
    picked, running = [], 0
    for lum, count, rgb in bins:
        picked.append({"hex": to_hex(rgb), "luminance": round(lum, 4)})
        running += count
        if running / total >= coverage:
            break
    return picked


def main():
    if shutil.which("convert") is None:
        sys.exit("ImageMagick (convert) is required")
    scrimmed("/tmp/_bd_scrimmed.png")
    mean = mean_colour("/tmp/_bd_scrimmed.png")
    print(json.dumps({
        "asset": ASSET,
        "scrim": {"top": SCRIM_TOP, "bottom": SCRIM_BOTTOM},
        "mean": to_hex(mean),
        "meanLuminance": round(luminance(mean), 4),
        "brightest5": brightest("/tmp/_bd_scrimmed.png"),
    }, indent=2))


if __name__ == "__main__":
    main()
