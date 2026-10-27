import Shared
import SwiftUI

/// Identifies a case of the dataset in a navigation path.
struct DatasetTarget: Hashable {
    let suite: String
    let id: String
}

/// The cases the next run uses. Tap one to change its input or its expected value.
struct DatasetView: View {
    @ObservedObject var model: SampleModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Change the input or the expected output of any case. The next run uses your changes, until the app restarts. \(model.editedCount) of \(model.cases.count) edited.")
                    .font(.subheadline)
                    .foregroundStyle(Palette.body)
                ForEach(suites, id: \.self) { suite in
                    VStack(alignment: .leading, spacing: 0) {
                        SectionLabel(text: suite).padding(.bottom, 8)
                        let cases = model.cases.filter { $0.suite == suite }
                        ForEach(Array(cases.enumerated()), id: \.offset) { index, item in
                            if index > 0 { Divider().overlay(Palette.hairline) }
                            NavigationLink(value: DatasetTarget(suite: item.suite, id: item.id)) {
                                DatasetRow(item: item).padding(.vertical, 10)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .card()
                }
                Button {
                    model.resetAllCases()
                } label: {
                    Text("Reset all to defaults").frame(maxWidth: .infinity, minHeight: 32)
                }
                .buttonStyle(.bordered)
                .tint(Palette.ink)
                .disabled(model.editedCount == 0)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
        .background(Palette.canvas.ignoresSafeArea())
        .navigationTitle("Cases")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var suites: [String] {
        var seen: [String] = []
        for item in model.cases where !seen.contains(item.suite) { seen.append(item.suite) }
        return seen
    }
}

private struct DatasetRow: View {
    let item: SampleCaseDef

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack {
                Text(item.id).font(.system(size: 14, weight: .semibold, design: .monospaced)).foregroundStyle(Palette.ink)
                Spacer()
                if item.edited { Text("EDITED").font(.system(size: 11, weight: .medium)).foregroundStyle(.blue) }
            }
            Text(item.input).font(.subheadline).foregroundStyle(Palette.body).lineLimit(2)
            Text("expected: \(item.expected ?? "none")")
                .font(.system(size: 12, design: .monospaced))
                .foregroundStyle(Palette.caption)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
    }
}

struct DatasetCaseView: View {
    let target: DatasetTarget
    @ObservedObject var model: SampleModel
    @SwiftUI.Environment(\.dismiss) private var dismiss
    @State private var input: String
    @State private var expected: String

    init(target: DatasetTarget, model: SampleModel) {
        self.target = target
        self.model = model
        let item = model.cases.first { $0.suite == target.suite && $0.id == target.id }
        _input = State(initialValue: item?.input ?? "")
        _expected = State(initialValue: item?.expected ?? "")
    }

    private var edited: Bool {
        model.cases.first { $0.suite == target.suite && $0.id == target.id }?.edited ?? false
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                SectionLabel(text: target.suite)
                field("Input", text: $input)
                field("Expected output", text: $expected)
                Text("Leave the expected output empty to run without an expected value.")
                    .font(.footnote)
                    .foregroundStyle(Palette.caption)
                Button {
                    model.saveCase(suite: target.suite, id: target.id, input: input, expected: expected)
                    dismiss()
                } label: {
                    Text("Save for the next run")
                        .font(.system(size: 15, weight: .semibold))
                        .frame(maxWidth: .infinity, minHeight: 32)
                }
                .buttonStyle(.borderedProminent)
                .tint(Palette.ink)
                .foregroundStyle(canSave ? Palette.onInk : Palette.caption)
                .disabled(!canSave)
                Button {
                    model.resetCase(suite: target.suite, id: target.id)
                    dismiss()
                } label: {
                    Text("Reset this case").frame(maxWidth: .infinity, minHeight: 32)
                }
                .buttonStyle(.bordered)
                .tint(Palette.ink)
                .disabled(!edited)
            }
            .card()
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
        .background(Palette.canvas.ignoresSafeArea())
        .navigationTitle(target.id)
        .navigationBarTitleDisplayMode(.inline)
    }

    private var canSave: Bool { !input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }

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
