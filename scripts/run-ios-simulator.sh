#!/usr/bin/env bash
# Runs the sample suites on Apple Foundation Models in an iOS simulator, with no Xcode project.
#
# The simulator uses the Foundation Models of its Mac host, so the Mac needs Apple Intelligence turned on. When it is
# not, the run still writes result files that record the availability instead of failing cases.
#
#   scripts/run-ios-simulator.sh [simulator name] [output directory]
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
device=${1:-iPhone 18 Pro}
out=${2:-$root/proctor-results}
sdk=$(xcrun --sdk iphonesimulator --show-sdk-path)
frameworks="$root/samples/shared/build/bin/iosSimulatorArm64/debugFramework"
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT

"$root/gradlew" -p "$root" :samples:shared:linkDebugFrameworkIosSimulatorArm64 --quiet

# The bridge imports the framework that exports proctor-ios; here that is Shared, not ProctorIos.
sed 's/^import ProctorIos$/import Shared/' "$root/proctor-ios/ios-bridge/ProctorFoundationModelsBridge.swift" > "$build/Bridge.swift"

swiftc -sdk "$sdk" -target arm64-apple-ios26.0-simulator -F "$frameworks" \
  -framework Shared -framework FoundationModels -framework UIKit \
  "$root/samples/ios-runner/main.swift" "$build/Bridge.swift" \
  -o "$build/proctor-runner"

udid=$(xcrun simctl list devices available | awk -v d="$device" 'index($0, d " (") { match($0, /[0-9A-F-]{36}/); print substr($0, RSTART, RLENGTH); exit }')
test -n "$udid" || { echo "No simulator named '$device'" >&2; exit 1; }
xcrun simctl bootstatus "$udid" -b > /dev/null
mkdir -p "$out"
xcrun simctl spawn "$udid" "$build/proctor-runner" "$out"
