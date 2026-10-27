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
                    CaseList(cases: result.cases)
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

struct CaseItem: View {
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
