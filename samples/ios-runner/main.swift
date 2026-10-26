// A headless runner for a simulator: no app, no Xcode project. `scripts/run-ios-simulator.sh` builds and runs it
// with `xcrun simctl spawn`. The same SampleRun call works from an XCTest in your own app target.

import Foundation
import Shared

let arguments = CommandLine.arguments
guard arguments.count >= 2 else {
    FileHandle.standardError.write(Data("usage: proctor-runner <output directory>\n".utf8))
    exit(2)
}

let bridge = ProctorFoundationModelsBridge()
print("Foundation Models availability: \(bridge.availability())")

do {
    let paths = try await SampleRun.shared.run(bridge: bridge, outputDirectory: arguments[1], requestDelayMillis: 500)
    paths.forEach { print("wrote \($0)") }
} catch {
    FileHandle.standardError.write(Data("run failed: \(error)\n".utf8))
    exit(1)
}
