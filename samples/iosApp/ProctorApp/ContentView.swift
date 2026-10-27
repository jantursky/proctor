import Shared
import SwiftUI

struct ContentView: View {
    @StateObject private var model = SampleModel()

    var body: some View {
        ScrollViewReader { proxy in
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                header
                modelCard
                evaluationCard
                if !model.rows.isEmpty { resultsCard.id("results") }
                Text("Prompts and outputs stay on this device. Nothing is sent anywhere.")
                    .font(.footnote)
                    .foregroundStyle(Palette.caption)
                    .padding(.top, 4)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
        .onChange(of: model.rows.count) {
            // `--expand` is for screenshots and tests: jump to the results as they arrive.
            if CommandLine.arguments.contains("--expand") { proxy.scrollTo("results", anchor: .top) }
        }
        }
        .background(Palette.canvas.ignoresSafeArea())
        .task {
            // `xcrun simctl launch <device> <bundle id> --autorun` starts a run without a tap.
            if CommandLine.arguments.contains("--autorun"), model.canRun { model.run() }
        }
    }

    private var header: some View {
        HStack(spacing: 14) {
            Image("Mark").resizable().scaledToFit().frame(width: 48, height: 48)
            VStack(alignment: .leading, spacing: 2) {
                Text("Proctor").font(.system(size: 28, weight: .semibold)).foregroundStyle(Palette.ink)
                Text("Evaluation harness for on-device LLMs").font(.subheadline).foregroundStyle(Palette.body)
            }
        }
    }

    private var modelCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionLabel(text: "Model")
            Text("foundation-models")
                .font(.system(size: 12, design: .monospaced))
                .foregroundStyle(Palette.caption)
            Divider().overlay(Palette.hairline)
            HStack(alignment: .top, spacing: 10) {
                Circle().fill(dot).frame(width: 10, height: 10).padding(.top, 6)
                VStack(alignment: .leading, spacing: 2) {
                    Text(model.status.title).font(.headline).foregroundStyle(Palette.ink)
                    if let detail = model.status.detail {
                        Text(detail).font(.subheadline).foregroundStyle(Palette.body)
                    }
                }
            }
            Button("Check again") { model.check() }
                .buttonStyle(.bordered)
                .tint(Palette.ink)
                .disabled(model.running)
        }
        .card()
    }

    private var evaluationCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionLabel(text: "Evaluation")
            Text("Runs ticket-classifier (prompt v1 and v2) and ticket-extraction, three attempts per case. Keep the app open while it runs.")
                .font(.subheadline)
                .foregroundStyle(Palette.body)
            Button {
                model.run()
            } label: {
                Text(model.running ? "Running..." : "Run sample suites")
                    .font(.system(size: 15, weight: .semibold))
                    .frame(maxWidth: .infinity, minHeight: 32)
            }
            .buttonStyle(.borderedProminent)
            .tint(Palette.ink)
            .foregroundStyle(model.canRun && !model.running ? Palette.onInk : Palette.caption)
            .disabled(!model.canRun || model.running)
            if model.running && model.total > 0 {
                ProgressView(value: Double(model.done), total: Double(model.total)).tint(Palette.ink)
                Text(progressText).font(.system(size: 12, design: .monospaced)).foregroundStyle(Palette.caption)
                if model.waiting {
                    Text("The system model asked the app to slow down. Waiting before the next try.")
                        .font(.footnote)
                        .foregroundStyle(Palette.notGraded)
                }
            }
            if let message = model.message {
                Text(message).font(.subheadline).foregroundStyle(Palette.ink)
            }
        }
        .card()
    }

    private var resultsCard: some View {
        VStack(alignment: .leading, spacing: 0) {
            SectionLabel(text: "Results").padding(.bottom, 8)
            ForEach(Array(model.rows.enumerated()), id: \.element.id) { index, row in
                if index > 0 { Divider().overlay(Palette.hairline) }
                ResultRow(row: row).padding(.vertical, 12)
            }
            Text("Files: \(model.resultsDirectory.path())")
                .font(.system(size: 12, design: .monospaced))
                .foregroundStyle(Palette.caption)
                .padding(.top, 8)
        }
        .card()
    }

    private var progressText: String {
        let percent = Int((Double(model.done) * 100 / Double(model.total)).rounded())
        var text = "\(model.done) of \(model.total) requests, \(percent)%"
        if let seconds = model.remainingSeconds {
            text += seconds >= 90 ? ", about \(Int((Double(seconds) / 60).rounded())) min left" : ", about \(seconds) s left"
        }
        return text
    }

    private var dot: Color {
        switch model.status.tone {
        case .good: Palette.pass
        case .warn: Palette.notGraded
        case .bad: Palette.fail
        case .neutral: Palette.caption
        }
    }
}

private struct ResultRow: View {
    let row: Row
    @State private var expanded = CommandLine.arguments.contains("--expand")

    var body: some View {
        let result = row.result
        let verdict = row.suitePassed ? Palette.pass : Palette.fail
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 0) {
                    Text(result.suite).font(.headline).foregroundStyle(Palette.ink)
                    Text("prompt \(result.promptVersion)")
                        .font(.system(size: 12, design: .monospaced))
                        .foregroundStyle(Palette.caption)
                }
                Spacer()
                Text(row.suitePassed ? "PASS" : "FAIL")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(verdict)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .overlay(RoundedRectangle(cornerRadius: 4).stroke(verdict, lineWidth: 1))
            }
            HStack(alignment: .firstTextBaseline, spacing: 14) {
                Text("\(result.passed)/\(result.total)").font(.system(size: 28, weight: .semibold)).foregroundStyle(Palette.ink)
                Text("passed").font(.subheadline).foregroundStyle(Palette.body)
                if result.failed > 0 { Text("\(result.failed) failed").font(.subheadline).foregroundStyle(Palette.fail) }
                if result.notGraded > 0 {
                    Text("\(result.notGraded) not graded").font(.subheadline).foregroundStyle(Palette.notGraded)
                }
            }
            if let score = result.meanScore?.doubleValue {
                HStack(spacing: 10) {
                    GeometryReader { proxy in
                        ZStack(alignment: .leading) {
                            Capsule().fill(Palette.hairline)
                            Capsule().fill(Palette.ink).frame(width: proxy.size.width * min(max(score, 0), 1))
                        }
                    }
                    .frame(height: 6)
                    Text("\(Int((score * 100).rounded()))%")
                        .font(.system(size: 12, design: .monospaced))
                        .foregroundStyle(Palette.body)
                }
            }
            Text(expanded ? "Hide cases" : "Show cases")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.blue)
            if expanded { CaseList(cases: result.cases) }
        }
        .contentShape(Rectangle())
        .onTapGesture { withAnimation { expanded.toggle() } }
    }
}

private enum CaseFilter: String, CaseIterable, Identifiable {
    case all = "All", failed = "Failed", passed = "Passed"
    var id: String { rawValue }
}

private struct CaseList: View {
    let cases: [SampleCase]
    @State private var filter: CaseFilter

    init(cases: [SampleCase]) {
        self.cases = cases
        _filter = State(initialValue: cases.contains { $0.status != .passed } ? .failed : .all)
    }

    private var shown: [SampleCase] {
        switch filter {
        case .all: cases
        case .failed: cases.filter { $0.status != .passed }
        case .passed: cases.filter { $0.status == .passed }
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Picker("Filter", selection: $filter) {
                ForEach(CaseFilter.allCases) { Text($0.rawValue).tag($0) }
            }
            .pickerStyle(.segmented)
            if shown.isEmpty {
                Text("No cases here.").font(.subheadline).foregroundStyle(Palette.body)
            }
            ForEach(shown, id: \.id) { CaseItem(item: $0) }
        }
    }
}

private struct CaseItem: View {
    let item: SampleCase

    private var tone: Color {
        switch item.status {
        case .passed: Palette.pass
        case .failed: Palette.fail
        default: Palette.notGraded
        }
    }

    private var label: String {
        switch item.status {
        case .passed: "PASS"
        case .failed: "FAIL"
        default: "NOT GRADED"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(item.id).font(.system(size: 14, weight: .semibold, design: .monospaced)).foregroundStyle(Palette.ink)
                Spacer()
                Text(label).font(.system(size: 11, weight: .medium)).foregroundStyle(tone)
            }
            Text(item.input).font(.subheadline).foregroundStyle(Palette.body)
            if let expected = item.expected { Detail(label: "Expected", value: expected) }
            ForEach(Array(item.attempts.enumerated()), id: \.offset) { index, attempt in
                Detail(label: item.attempts.count > 1 ? "Attempt \(index + 1)" : "Output", value: attempt.output ?? "(no output)")
                if let reason = attempt.reason { Detail(label: "Reason", value: reason) }
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 8).fill(Palette.band))
    }
}

private struct Detail: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 1) {
            Text(label.uppercased()).font(.system(size: 11, weight: .medium)).foregroundStyle(Palette.caption)
            Text(value).font(.system(size: 12, design: .monospaced)).foregroundStyle(Palette.ink)
        }
    }
}
