package com.jantursky.proctor.report

import com.jantursky.proctor.RunResult
import com.jantursky.proctor.Status

/** One self-contained page: styles inline, no scripts, no network requests. */
internal object HtmlReport {
    fun render(report: Report): String =
        buildString {
            appendLine("<!doctype html>")
            appendLine("<html lang=\"en\"><head><meta charset=\"utf-8\">")
            appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
            appendLine("<title>Proctor report</title><style>$STYLE</style></head><body><main>")
            appendLine("<h1>Proctor report</h1>")
            checkedOn(report)
            results(report)
            report.diffs.forEach { diff(it) }
            report.runs.forEach { cases(it) }
            appendLine("</main></body></html>")
        }

    private fun StringBuilder.checkedOn(report: Report) {
        appendLine("<h2>Checked on</h2><div class=\"scroll\"><table><thead><tr>")
        listOf("Suite", "Platform", "Device", "OS", "Model version", "Backend", "Prompt", "Availability", "Run at")
            .forEach { appendLine("<th>${esc(it)}</th>") }
        appendLine("</tr></thead><tbody>")
        for (run in report.runs) {
            val e = run.environment
            val availability = run.availability.name.lowercase() + (run.availabilityReason?.let { " ($it)" } ?: "")
            appendLine("<tr>")
            appendLine("<td>${esc(run.suite)}</td><td>${esc(e.platform)}</td><td>${esc(e.deviceModel)}</td>")
            appendLine("<td>${esc(e.osVersion)}</td><td class=\"mono\">${esc(e.modelVersion ?: "unknown")}</td>")
            appendLine("<td>${esc(e.backend)}</td><td class=\"mono\">${esc(run.promptVersion)}</td>")
            appendLine("<td>${esc(availability)}</td><td class=\"mono\">${esc(run.startedAt)}</td>")
            appendLine("</tr>")
        }
        appendLine("</tbody></table></div>")
    }

    private fun StringBuilder.results(report: Report) {
        appendLine("<h2>Results</h2><div class=\"scroll\"><table><thead><tr>")
        listOf("Run", "Cases", "Passed", "Failed", "Not graded", "Mean score", "Verdict")
            .forEach { appendLine("<th>${esc(it)}</th>") }
        appendLine("</tr></thead><tbody>")
        for (run in report.runs) {
            val s = run.summary
            appendLine("<tr><td>${esc(label(run))}</td><td>${s.total}</td><td>${s.passed}</td><td>${s.failed}</td>")
            appendLine("<td>${s.notGraded}</td><td>${s.meanScore.percent()}</td>")
            appendLine("<td>${tag(if (s.suitePassed) Status.Passed else Status.Failed)}</td></tr>")
        }
        appendLine("</tbody></table></div>")
        appendLine(
            "<p class=\"note\">Not graded means no verdict, for example the quota ran out. It is not a failure.</p>",
        )
    }

    private fun StringBuilder.diff(diff: RunDiff) {
        appendLine("<h2>Compare: ${esc(diff.base.suite)}</h2>")
        appendLine("<p><span class=\"mono\">${esc(label(diff.base))}</span> against ")
        appendLine("<span class=\"mono\">${esc(label(diff.other))}</span></p>")
        appendLine("<p>Differs in ${esc(diff.describe())}.</p>")
        appendLine("<p>Score ${diff.base.summary.meanScore.percent()} to ${diff.other.summary.meanScore.percent()} ")
        appendLine(
            "(${esc(diff.scoreDelta.signed())}). ${diff.regressions} regressed, ${diff.improvements} improved.</p>",
        )
        appendLine("<div class=\"scroll\"><table><thead><tr><th>Case</th><th>Input</th><th>Base</th><th>Other</th>")
        appendLine("<th>Change</th></tr></thead><tbody>")
        for (case in diff.cases) {
            appendLine("<tr><td class=\"mono\">${esc(case.id)}</td><td>${esc(case.input)}</td>")
            appendLine("<td>${case.base?.let { tag(it.status) } ?: "–"}</td>")
            appendLine("<td>${case.other?.let { tag(it.status) } ?: "–"}</td>")
            appendLine("<td>${esc(case.change.name)}</td></tr>")
        }
        appendLine("</tbody></table></div>")
    }

    private fun StringBuilder.cases(run: RunResult) {
        appendLine("<h2>Cases: ${esc(label(run))}</h2>")
        for (case in run.cases) {
            appendLine("<details><summary>${tag(case.status)} <span class=\"mono\">${esc(case.id)}</span> ")
            appendLine("${esc(case.input)} <span class=\"muted\">score ${case.meanScore.percent()}, ")
            appendLine("spread ${case.spread.twoDecimals()}</span></summary>")
            case.expected?.let { appendLine("<p>Expected <span class=\"mono\">${esc(it)}</span></p>") }
            case.attempts.forEachIndexed { index, attempt ->
                appendLine("<div class=\"attempt\"><strong>Attempt ${index + 1}</strong> ${tag(attempt.status)}")
                attempt.output?.let { appendLine("<pre>${esc(it)}</pre>") }
                attempt.note?.let { appendLine("<p class=\"muted\">${esc(it)}</p>") }
                attempt.graders.forEach {
                    appendLine(
                        "<p>${tag(it.status)} <span class=\"mono\">${esc(it.name)}</span> ${esc(it.rationale)}</p>",
                    )
                }
                appendLine("</div>")
            }
            appendLine("</details>")
        }
    }

    private fun tag(status: Status): String {
        val css =
            when (status) {
                Status.Passed -> "pass"
                Status.Failed -> "fail"
                Status.NotGraded -> "ng"
            }
        return "<span class=\"tag $css\">${esc(status.symbol())}</span>"
    }

    private fun esc(text: String): String =
        buildString(text.length) {
            for (c in text) {
                when (c) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&#39;")
                    else -> append(c)
                }
            }
        }

    // Tokens from design/tokens.css, inlined so the report is a single file.
    private val STYLE = """
:root{--canvas:#fff;--band:#f3f4f6;--hairline:#e5e7eb;--ink:#000;--body:#4a5565;--caption:#6a7282;--link:#155dfc;--pass:#16794c;--fail:#b42318;--ng:#a15c07}
@media (prefers-color-scheme:dark){:root{--canvas:#0b0f19;--band:#101828;--hairline:#2c3344;--ink:#f9fafb;--body:#d1d5dc;--caption:#99a1af;--link:#51a2ff;--pass:#4ade80;--fail:#f87171;--ng:#fbbf24}}
*{box-sizing:border-box}body{margin:0;background:var(--canvas);color:var(--body);font:14px/1.5 'Source Sans 3','Source Sans Pro',Inter,system-ui,sans-serif}
main{max-width:1200px;margin:0 auto;padding:32px 16px}h1{font-size:30px;line-height:1.25;color:var(--ink);margin:0 0 24px}
h2{font-size:20px;color:var(--ink);margin:40px 0 12px}.mono,pre{font-family:'IBM Plex Mono',ui-monospace,monospace;font-size:13px}
.scroll{overflow-x:auto}table{border-collapse:collapse;width:100%}th,td{text-align:left;padding:8px 12px;border-bottom:1px solid var(--hairline);vertical-align:top}
th{font-weight:600;color:var(--ink);background:var(--band)}.tag{display:inline-block;padding:1px 8px;border-radius:4px;font-size:12px;font-weight:600;background:var(--band)}
.pass{color:var(--pass)}.fail{color:var(--fail)}.ng{color:var(--ng)}.muted,.note{color:var(--caption)}
details{border:1px solid var(--hairline);border-radius:8px;padding:8px 16px;margin:8px 0}summary{cursor:pointer}
pre{white-space:pre-wrap;background:var(--band);border-radius:4px;padding:8px 12px;margin:8px 0}.attempt{margin:12px 0}
"""
}
