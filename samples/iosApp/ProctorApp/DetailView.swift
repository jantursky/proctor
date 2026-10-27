import Shared
import SwiftUI

/// The details of one run: summary, where it ran, and its cases.
struct DetailView: View {
    let row: Row

    private var result: SampleRow { row.result }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 14) {
                    SectionLabel(text: "Run")
                    HStack {
                        Text("prompt \(result.promptVersion)")
                            .font(.system(size: 14, design: .monospaced))
                            .foregroundStyle(Palette.body)
                        Spacer()
                        VerdictBadge(passed: result.suitePassed)
                    }
                    HStack(alignment: .firstTextBaseline, spacing: 14) {
                        Text("\(result.passed)/\(result.total)").font(.system(size: 28, weight: .semibold)).foregroundStyle(Palette.ink)
                        Text("passed").font(.subheadline).foregroundStyle(Palette.body)
                        if result.failed > 0 { Text("\(result.failed) failed").font(.subheadline).foregroundStyle(Palette.fail) }
                        if result.notGraded > 0 {
                            Text("\(result.notGraded) not graded").font(.subheadline).foregroundStyle(Palette.notGraded)
                        }
                    }
                    Divider().overlay(Palette.hairline)
                    Detail(label: "Started", value: formatStarted(result.startedAt))
                    Detail(label: "Device", value: "\(result.deviceModel), \(result.osVersion)")
                    Detail(label: "Model version", value: result.modelVersion ?? "not reported")
                    Detail(label: "Backend", value: result.backend)
                    if let note = result.availabilityNote { Detail(label: "Availability", value: note) }
                }
                .card()
                VStack(alignment: .leading, spacing: 14) {
                    SectionLabel(text: "Cases")
                    CaseList(cases: result.cases, run: result)
                }
                .card()
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
        .background(Palette.canvas.ignoresSafeArea())
        .navigationTitle(result.suite)
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct VerdictBadge: View {
    let passed: Bool

    var body: some View {
        let tone = passed ? Palette.pass : Palette.fail
        Text(passed ? "PASS" : "FAIL")
            .font(.system(size: 11, weight: .medium))
            .foregroundStyle(tone)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .overlay(RoundedRectangle(cornerRadius: 4).stroke(tone, lineWidth: 1))
    }
}

/// Turns a result file's start stamp into `2026-10-27 01:03 UTC`.
func formatStarted(_ startedAt: String) -> String {
    let date = startedAt.split(separator: "T").first.map(String.init) ?? startedAt
    let time = startedAt.split(separator: "T").dropFirst().first.map { String($0.prefix(5)).replacingOccurrences(of: "-", with: ":") } ?? ""
    return "\(date) \(time) UTC"
}

enum CaseFilter: String, CaseIterable, Identifiable {
    case all = "All", failed = "Failed", passed = "Passed"
    var id: String { rawValue }
}

struct CaseList: View {
    let cases: [SampleCase]
    let run: SampleRow?
    @State private var filter: CaseFilter

    init(cases: [SampleCase], run: SampleRow? = nil) {
        self.cases = cases
        self.run = run
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
            ForEach(shown, id: \.id) { CaseItem(item: $0, run: run) }
        }
    }
}

struct CaseItem: View {
    let item: SampleCase
    var run: SampleRow?

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
            if let run {
                NavigationLink(value: EditTarget(run: run, item: item)) {
                    Text("Edit and run").font(.system(size: 14, weight: .semibold)).foregroundStyle(.blue)
                }
                .padding(.top, 4)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 8).fill(Palette.band))
    }
}

struct Detail: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 1) {
            Text(label.uppercased()).font(.system(size: 11, weight: .medium)).foregroundStyle(Palette.caption)
            Text(value).font(.system(size: 12, design: .monospaced)).foregroundStyle(Palette.ink)
        }
    }
}

/// What the edit screen needs: the run a case came from and the case itself.
struct EditTarget: Hashable {
    let run: SampleRow
    let item: SampleCase

    static func == (lhs: EditTarget, rhs: EditTarget) -> Bool { lhs.item.id == rhs.item.id && lhs.run.startedAt == rhs.run.startedAt && lhs.run.promptVersion == rhs.run.promptVersion && lhs.run.suite == rhs.run.suite }
    func hash(into hasher: inout Hasher) {
        hasher.combine(item.id)
        hasher.combine(run.startedAt)
        hasher.combine(run.suite)
        hasher.combine(run.promptVersion)
    }
}

/// Change the input and the expected answer of a case and run it again on Foundation Models.
struct EditCaseView: View {
    let target: EditTarget
    @EnvironmentObject var model: SampleModel
    @State private var input: String
    @State private var expected: String

    init(target: EditTarget) {
        self.target = target
        _input = State(initialValue: target.item.input)
        _expected = State(initialValue: target.item.expected ?? "")
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 14) {
                    SectionLabel(text: "\(target.run.suite) · prompt \(target.run.promptVersion)")
                    Text("Runs on Foundation Models with the same prompt version and graders. Nothing is saved.")
                        .font(.subheadline)
                        .foregroundStyle(Palette.body)
                    field("Input", text: $input)
                    field("Expected output", text: $expected)
                    Text("Leave the expected output empty to run without an expected value.")
                        .font(.footnote)
                        .foregroundStyle(Palette.caption)
                    Button {
                        model.runCustom(row: target.run, input: input, expected: expected)
                    } label: {
                        Text(model.customRunning ? "Running..." : "Run this case")
                            .font(.system(size: 15, weight: .semibold))
                            .frame(maxWidth: .infinity, minHeight: 32)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Palette.ink)
                    .foregroundStyle(canRun ? Palette.onInk : Palette.caption)
                    .disabled(!canRun)
                    if !model.canRun {
                        Text("The model is not available. Check it on the home screen.")
                            .font(.footnote)
                            .foregroundStyle(Palette.notGraded)
                    }
                }
                .card()
                if let result = model.customResult {
                    VStack(alignment: .leading, spacing: 14) {
                        SectionLabel(text: "Result")
                        CaseItem(item: result)
                    }
                    .card()
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
        .background(Palette.canvas.ignoresSafeArea())
        .navigationTitle("Edit case")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { model.customResult = nil }
    }

    private var canRun: Bool {
        model.canRun && !model.customRunning && !input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private func field(_ label: String, text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label.uppercased()).font(.system(size: 11, weight: .medium)).foregroundStyle(Palette.caption)
            TextField(label, text: text, axis: .vertical)
                .font(.system(size: 14, design: .monospaced))
                .padding(10)
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Palette.hairline, lineWidth: 1))
        }
    }
}
