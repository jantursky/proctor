import Shared
import SwiftUI

/// Runs saved by earlier sessions, newest first.
struct HistoryView: View {
    @ObservedObject var model: SampleModel
    @State private var confirmClear = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if model.history.isEmpty {
                    Text("No saved runs yet. Every run is saved as a result file on this device and listed here.")
                        .font(.subheadline)
                        .foregroundStyle(Palette.body)
                } else {
                    ForEach(model.historyGroups, id: \.startedAt) { group in
                        GroupCard(group: group, model: model)
                    }
                    Button(role: .destructive) {
                        confirmClear = true
                    } label: {
                        Text("Clear history").frame(maxWidth: .infinity, minHeight: 32)
                    }
                    .buttonStyle(.bordered)
                    .tint(Palette.fail)
                    .disabled(model.running)
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
        .background(Palette.canvas.ignoresSafeArea())
        .navigationTitle("History")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { model.loadHistory() }
        .confirmationDialog(
            "Clear history?",
            isPresented: $confirmClear,
            titleVisibility: .visible
        ) {
            Button("Delete all \(model.history.count) saved runs", role: .destructive) { model.clearHistory() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This deletes the result files on this device. It cannot be undone.")
        }
    }
}

private struct GroupCard: View {
    let group: SampleGroup
    @ObservedObject var model: SampleModel

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            SectionLabel(text: group.id == nil ? "Ungrouped runs" : formatStarted(group.startedAt))
            Text("\(group.rows.count) \(group.rows.count == 1 ? "run" : "runs") · \(group.backend)" + (group.modelVersion.map { " · \($0)" } ?? ""))
                .font(.system(size: 12, design: .monospaced))
                .foregroundStyle(Palette.caption)
            VStack(alignment: .leading, spacing: 0) {
                ForEach(Array(group.rows.enumerated()), id: \.offset) { index, result in
                    if index > 0 { Divider().overlay(Palette.hairline) }
                    NavigationLink(value: Row(result: result)) { HistoryRow(row: Row(result: result)).padding(.vertical, 12) }
                        .buttonStyle(.plain)
                }
            }
        }
        .card()
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
            Text(formatStarted(result.startedAt))
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
