#!/usr/bin/env bash
# Builds the iOS sample app, installs it on a simulator and starts it. Pass --autorun to start the suites at launch.
# The simulator uses the Foundation Models of its Mac host, so the Mac needs Apple Intelligence turned on.
#
#   scripts/run-ios-app.sh [--autorun] [simulator name]
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
autorun=""
if [ "${1:-}" = "--autorun" ]; then autorun="--autorun"; shift; fi
device=${1:-iPhone 18 Pro}
bundle=com.jantursky.proctor.sample.app

command -v xcodegen > /dev/null || { echo "Install XcodeGen first: brew install xcodegen" >&2; exit 1; }
(cd "$root/samples/iosApp" && xcodegen generate --quiet)

xcodebuild -project "$root/samples/iosApp/ProctorApp.xcodeproj" -scheme ProctorApp -sdk iphonesimulator \
  -destination "platform=iOS Simulator,name=$device" -derivedDataPath "$root/samples/iosApp/build" build -quiet

udid=$(xcrun simctl list devices available | awk -v d="$device" 'index($0, d " (") { match($0, /[0-9A-F-]{36}/); print substr($0, RSTART, RLENGTH); exit }')
test -n "$udid" || { echo "No simulator named '$device'" >&2; exit 1; }
xcrun simctl bootstatus "$udid" -b > /dev/null
open -a Simulator
xcrun simctl terminate "$udid" "$bundle" 2> /dev/null || true
xcrun simctl install "$udid" "$root/samples/iosApp/build/Build/Products/Debug-iphonesimulator/ProctorApp.app"
xcrun simctl launch "$udid" "$bundle" $autorun
