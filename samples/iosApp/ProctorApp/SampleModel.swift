import Foundation
import Shared
import SwiftUI

struct ModelStatus {
    enum Tone { case neutral, good, warn, bad }
    var title: String
    var detail: String?
    var tone: Tone
}

struct Row: Identifiable, Hashable {
    let id = UUID()
    let result: SampleRow

    static func == (lhs: Row, rhs: Row) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }

    var suitePassed: Bool { result.suitePassed }
}

@MainActor
final class SampleModel: ObservableObject {
    @Published var status = ModelStatus(title: "Checking Apple Foundation Models", detail: nil, tone: .neutral)
    @Published var canRun = false
    @Published var running = false
    @Published var done = 0
    @Published var total = 0
    @Published var waiting = false
    @Published var remainingSeconds: Int?
    @Published var message: String?
    @Published var rows: [Row] = []
    @Published var history: [Row] = []
    @Published var customRunning = false
    @Published var customResult: SampleCase?

    private let bridge = ProctorFoundationModelsBridge()
    private let edits = SampleEdits()
    @Published var cases: [SampleCaseDef] = []

    var editedCount: Int { cases.filter(\.edited).count }

    func saveCase(suite: String, id: String, input: String, expected: String) {
        edits.set(suite: suite, id: id, input: input, expected: expected)
        cases = edits.cases()
    }

    func resetCase(suite: String, id: String) {
        edits.reset(suite: suite, id: id)
        cases = edits.cases()
    }

    func resetAllCases() {
        edits.resetAll()
        cases = edits.cases()
    }

    var resultsDirectory: URL {
        URL.documentsDirectory.appending(path: "proctor")
    }

    init() {
        cases = edits.cases()
        check()
    }

    /// Reads every result file the app has written. A file that cannot be read is skipped.
    func loadHistory() {
        let files = (try? FileManager.default.contentsOfDirectory(at: resultsDirectory, includingPropertiesForKeys: nil)) ?? []
        let parsed = files
            .filter { $0.lastPathComponent.hasSuffix(".proctor.json") }
            .compactMap { try? String(contentsOf: $0, encoding: .utf8) }
            .compactMap { SampleReportKt.parseSampleRow(json: $0) }
            .sorted { $0.startedAt > $1.startedAt }
        history = parsed.map { Row(result: $0) }
    }

    func check() {
        switch bridge.availability() {
        case .available:
            status = ModelStatus(title: "Available", detail: "Ready to run.", tone: .good)
        case .deviceNotEligible:
            status = ModelStatus(
                title: "Unavailable",
                detail: "This device is not eligible for Apple Intelligence. On a simulator, the Mac host must be.",
                tone: .bad
            )
        case .appleIntelligenceNotEnabled:
            status = ModelStatus(
                title: "Apple Intelligence is off",
                detail: "Turn it on in Settings, then check again. On a simulator, turn it on on the Mac.",
                tone: .warn
            )
        case .modelNotReady:
            status = ModelStatus(
                title: "Model not ready",
                detail: "The model is still downloading or preparing. Check again in a moment.",
                tone: .warn
            )
        default:
            status = ModelStatus(title: "Unknown state", detail: nil, tone: .warn)
        }
        canRun = bridge.availability() == .available
    }

    /// History grouped by the runs that were started together, newest group first.
    var historyGroups: [SampleGroup] {
        SampleReportKt.groupHistory(rows: history.map(\.result))
    }

    /// Deletes every saved result file and the lists built from them. Does nothing while a run is active.
    func clearHistory() {
        guard !running else { return }
        let files = (try? FileManager.default.contentsOfDirectory(at: resultsDirectory, includingPropertiesForKeys: nil)) ?? []
        for file in files where file.lastPathComponent.hasSuffix(".proctor.json") {
            try? FileManager.default.removeItem(at: file)
        }
        history = []
        rows = []
        message = nil
    }

    /// Runs the user's own version of a case on Foundation Models, without saving anything.
    func runCustom(row: SampleRow, input: String, expected: String) {
        guard !customRunning else { return }
        customRunning = true
        customResult = nil
        Task {
            do {
                customResult = try await SampleCustom.shared.run(
                    bridge: bridge,
                    suiteName: row.suite,
                    promptVersion: row.promptVersion,
                    input: input,
                    expected: expected
                )
            } catch {
                message = "Run aborted. \(error.localizedDescription)"
            }
            customRunning = false
        }
    }

    func run() {
        guard !running else { return }
        running = true
        rows = []
        message = nil
        done = 0
        total = Int(SampleReportKt.sampleRequestCount)
        waiting = false
        remainingSeconds = nil
        let startedAt = Date()
        let listener = Listener(self, startedAt: startedAt)
        Task {
            do {
                let written = try await SampleSession.shared.run(
                    bridge: bridge,
                    outputDirectory: resultsDirectory.path(),
                    groupId: UUID().uuidString,
                    edits: edits,
                    listener: listener
                )
                message = "Done. \(written) result files written."
            } catch {
                message = "Run aborted. \(error.localizedDescription)"
            }
            running = false
            total = 0
            remainingSeconds = nil
        }
    }

    fileprivate func add(_ row: SampleRow) { rows.append(Row(result: row)) }

    fileprivate func update(done: Int, total: Int, startedAt: Date) {
        self.done = done
        self.total = total
        waiting = false
        let elapsed = Date().timeIntervalSince(startedAt)
        remainingSeconds = done > 0 ? Int(elapsed * Double(total - done) / Double(done)) : nil
    }

    fileprivate func setWaiting() { waiting = true }
}

/// Kotlin calls this from its own threads; hop to the main actor before touching the UI.
private final class Listener: NSObject, SampleListener {
    private weak var model: SampleModel?
    private let startedAt: Date

    init(_ model: SampleModel, startedAt: Date) {
        self.model = model
        self.startedAt = startedAt
    }

    func onProgress(done: Int32, total: Int32) {
        let started = startedAt
        Task { @MainActor in model?.update(done: Int(done), total: Int(total), startedAt: started) }
    }

    func onWaiting() {
        Task { @MainActor in model?.setWaiting() }
    }

    func onResult(row: SampleRow) {
        Task { @MainActor in model?.add(row) }
    }
}
