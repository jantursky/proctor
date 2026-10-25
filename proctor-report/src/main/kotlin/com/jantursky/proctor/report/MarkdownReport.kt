package com.jantursky.proctor.report

import com.jantursky.proctor.RunResult
import com.jantursky.proctor.Status

internal object MarkdownReport {
    private const val OUTPUT_LIMIT = 160

    fun render(report: Report): String =
        buildString {
            appendLine("# Proctor report")
            appendLine()
            appendLine("## Checked on")
            appendLine()
            appendLine("| Suite | Platform | Device | OS | Model version | Backend | Prompt | Availability | Run at |")
            appendLine("| --- | --- | --- | --- | --- | --- | --- | --- | --- |")
            for (run in report.runs) {
                val e = run.environment
                appendLine(
                    row(
                        run.suite,
                        e.platform,
                        e.deviceModel,
                        e.osVersion,
                        e.modelVersion ?: "unknown",
                        e.backend,
                        run.promptVersion,
                        run.availability.name.lowercase() + (run.availabilityReason?.let { " ($it)" } ?: ""),
                        run.startedAt,
                    ),
                )
            }
            appendLine()
            appendLine("## Results")
            appendLine()
            appendLine("| Run | Cases | Passed | Failed | Not graded | Mean score | Verdict |")
            appendLine("| --- | --- | --- | --- | --- | --- | --- |")
            for (run in report.runs) {
                val s = run.summary
                appendLine(
                    row(
                        label(run),
                        "${s.total}",
                        "${s.passed}",
                        "${s.failed}",
                        "${s.notGraded}",
                        s.meanScore.percent(),
                        if (s.suitePassed) "pass" else "fail",
                    ),
                )
            }
            report.diffs.forEach { diff(it) }
            report.runs.forEach { cases(it) }
        }

    private fun StringBuilder.diff(diff: RunDiff) {
        appendLine()
        appendLine("## Compare: ${diff.base.suite}")
        appendLine()
        appendLine("Base ${label(diff.base)} against ${label(diff.other)}.")
        appendLine("Differs in ${diff.describe()}.")
        appendLine()
        appendLine(
            "Score ${diff.base.summary.meanScore.percent()} to ${diff.other.summary.meanScore.percent()} " +
                "(${diff.scoreDelta.signed()}). ${diff.regressions} regressed, ${diff.improvements} improved.",
        )
        appendLine()
        appendLine("| Case | Base | Other | Change |")
        appendLine("| --- | --- | --- | --- |")
        for (case in diff.cases) {
            appendLine(
                row(
                    case.id,
                    case.base?.status?.symbol() ?: "–",
                    case.other?.status?.symbol() ?: "–",
                    case.change.name,
                ),
            )
        }
    }

    private fun StringBuilder.cases(run: RunResult) {
        appendLine()
        appendLine("## Cases: ${label(run)}")
        appendLine()
        appendLine("| Case | Input | Expected | Output (first attempt) | Result | Score | Spread |")
        appendLine("| --- | --- | --- | --- | --- | --- | --- |")
        for (case in run.cases) {
            val first = case.attempts.firstOrNull()
            val output = first?.output ?: first?.note?.let { "($it)" } ?: ""
            appendLine(
                row(
                    case.id,
                    case.input,
                    case.expected ?: "",
                    output,
                    case.status.symbol(),
                    case.meanScore.percent(),
                    case.spread.twoDecimals(),
                ),
            )
        }
        val notGraded = run.cases.filter { it.status == Status.NotGraded }
        if (notGraded.isNotEmpty()) {
            appendLine()
            appendLine("Not graded cases are not prompt failures: ${notGraded.joinToString { it.id }}.")
        }
    }

    private fun row(vararg cells: String): String = cells.joinToString(" | ", "| ", " |") { cell(it) }

    private fun cell(text: String): String {
        val flat = text.replace(Regex("\\s+"), " ").trim()
        val short = if (flat.length > OUTPUT_LIMIT) flat.take(OUTPUT_LIMIT) + "…" else flat
        return short.replace("|", "\\|")
    }
}
