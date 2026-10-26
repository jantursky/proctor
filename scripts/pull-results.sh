#!/usr/bin/env bash
# Pulls the result files a device run wrote (see SuiteRunTest) into ./proctor-results.
# Usage: scripts/pull-results.sh [package]   (default: the sample app)
set -euo pipefail

package="${1:-com.jantursky.proctor.sample.app}"
target="$(cd "$(dirname "$0")/.." && pwd)/proctor-results"

mkdir -p "$target"
adb pull "/sdcard/Android/data/$package/files/proctor/." "$target"
echo "Result files are in $target"
