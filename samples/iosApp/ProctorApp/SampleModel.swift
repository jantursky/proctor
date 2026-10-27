import Foundation
import Shared
import SwiftUI

struct ModelStatus {
    enum Tone { case neutral, good, warn, bad }
    var title: String
    var detail: String?
    var tone: Tone
}

struct Row: Identifiable {
    let id = UUID()
    let suite: String
    let promptVersion: String
    let passed: Int
    let failed: Int
    let notGraded: Int
    let total: Int
    let meanScore: Double?
    let suitePassed: Bool
}

@MainActor
final class SampleModel: ObservableObject {
    @Published var status = ModelStatus(title: "Checking Apple Foundation Models", detail: nil, tone: .neutral)
    @Published var canRun = false
    @Published var running = false
    @Published var progress: String?
    @Published var message: String?
    @Published var rows: [Row] = []

    private let bridge = ProctorFoundationModelsBridge()

    var resultsDirectory: URL {
        URL.documentsDirectory.appending(path: "proctor")
    }

    init() { check() }

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

    func run() {
        guard !running else { return }
        running = true
        rows = []
        message = nil
        let listener = Listener(self)
        Task {
            do {
                let written = try await SampleSession.shared.run(
                    bridge: bridge,
                    outputDirectory: resultsDirectory.path(),
                    listener: listener
                )
                message = "Done. \(written) result files written."
            } catch {
                message = "Run aborted. \(error.localizedDescription)"
            }
            running = false
            progress = nil
        }
    }

    fileprivate func add(_ row: SampleRow) {
        rows.append(Row(
            suite: row.suite,
            promptVersion: row.promptVersion,
            passed: Int(row.passed),
            failed: Int(row.failed),
            notGraded: Int(row.notGraded),
            total: Int(row.total),
            meanScore: row.meanScore?.doubleValue,
            suitePassed: row.suitePassed
        ))
    }

    fileprivate func setProgress(_ text: String) { progress = text }
}

/// Kotlin calls this from its own threads; hop to the main actor before touching the UI.
private final class Listener: NSObject, SampleListener {
    private weak var model: SampleModel?
    init(_ model: SampleModel) { self.model = model }

    func onProgress(text: String) {
        Task { @MainActor in model?.setProgress(text) }
    }

    func onResult(row: SampleRow) {
        Task { @MainActor in model?.add(row) }
    }
}
