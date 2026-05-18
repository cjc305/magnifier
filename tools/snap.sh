#!/usr/bin/env bash
# Helper for capturing screenshots on the Galaxy A13 (SM-A136B).
# Usage:  ./tools/snap.sh <name>
# Output: docs/play-store/assets/screenshots/<name>.png
#
# Workflow:
#   1. Manually navigate the app on the device to the state you want
#   2. Run: ./tools/snap.sh main-noir
#   3. Repeat for each state
#
# Why manual navigation: Compose UIs don't expose semantics through
# `uiautomator dump`, so automated tapping by coords is fragile —
# Samsung's gesture nav zone overlaps the bottom of our floating capsule.

set -e

DEVICE="${ADB_DEVICE:-R5CT814DJWV}"
NAME="${1:?usage: ./tools/snap.sh <name>}"
OUT_DIR="docs/play-store/assets/screenshots"
mkdir -p "$OUT_DIR"

adb -s "$DEVICE" exec-out screencap -p > "$OUT_DIR/$NAME.png"
echo "$OUT_DIR/$NAME.png  ($(stat -c%s "$OUT_DIR/$NAME.png") bytes)"
