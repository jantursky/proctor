import Shared
import SwiftUI

/// Runs saved by earlier sessions, newest first.
struct HistoryView: View {
    @ObservedObject var model: SampleModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if model.history.isEmpty {
                    Text("No saved runs yet. Every run is saved as a result file on this device and listed here.")
                        .font(.subheadline)
                        .foregroundStyle(Palette.body)
                } else {
                    VStack(alignment: .leading, spacing: 0) {
                        SectionLabel(text: "\(model.history.count) saved runs").padding(.bottom, 8)
                        ForEach(Array(model.history.enumerated()), id: \.element.id) { index, row in
                            if index > 0 { Divider().overlay(Palette.hairline) }
                            NavigationLink(value: row) { HistoryRow(row: row).padding(.vertical, 12) }
                                .buttonStyle(.plain)
                        }
                    }
                    .card()
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
        .background(Palette.canvas.ignoresSafeArea())
        .navigationTitle("History")
        .navigationBarTitleDisplayMode(.inline)
        .navigationDestination(for: Row.self) { DetailView(row: $0) }
        .onAppear { model.loadHistory() }
    }
}

private struct HistoryRow: View {
    let row: Row

    var body: some View {
        let result = row.result
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text("\(result.suite) · \(result.promptVersion)").font(.headline).foregroundStyle(Palette.ink)
                Spacer()
                VerdictBadge(passed: result.suitePassed)
            }
            Text(summary(result)).font(.subheadline).foregroundStyle(Palette.body)
            Text("\(formatStarted(result.startedAt)) · \(result.backend)")
                .font(.system(size: 12, design: .monospaced))
                .foregroundStyle(Palette.caption)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
    }

    private func summary(_ result: SampleRow) -> String {
        var text = "\(result.passed)/\(result.total) passed"
        if result.failed > 0 { text += ", \(result.failed) failed" }
        if result.notGraded > 0 { text += ", \(result.notGraded) not graded" }
        return text
    }
}
