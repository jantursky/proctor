package com.jantursky.proctor.report

import com.jantursky.proctor.RunResult
import com.jantursky.proctor.Status

/** What turns a report into a failing CI step. A run whose own `passWhen` verdict is false always fails. */
public data class VerdictOptions(
    /** Also fail when a case that passed in the baseline fails in another run. */
    val failOnRegression: Boolean = false,
    /** Also fail when any case could not be graded, for example because of quota. */
    val failOnNotGraded: Boolean = false,
)

public class Verdict internal constructor(
    public val passed: Boolean,
    public val reasons: List<String>,
)

/**
 * Runs of one or more suites, with each suite's runs compared against its baseline.
 *
 * Nothing is uploaded or sent anywhere: the report is built from local files.
 */
public class Report private constructor(
    public val runs: List<RunResult>,
    public val diffs: List<RunDiff>,
) {
    public fun toMarkdown(): String = MarkdownReport.render(this)

    public fun toHtml(): String = HtmlReport.render(this)

    public fun verdict(options: VerdictOptions = VerdictOptions()): Verdict {
        val reasons =
            buildList {
                for (run in runs) {
                    if (!run.summary.suitePassed) add("${label(run)} did not meet its pass condition")
                    if (options.failOnNotGraded && run.summary.notGraded > 0) {
                        add("${label(run)} has ${run.summary.notGraded} case(s) not graded")
                    }
                }
                if (options.failOnRegression) {
                    for (diff in diffs.filter { it.regressions > 0 }) {
                        add("${label(diff.other)} regressed on ${diff.regressions} case(s) against ${label(diff.base)}")
                    }
                }
            }
        return Verdict(reasons.isEmpty(), reasons)
    }

    public companion object {
        /**
         * Builds a report. Within each suite the [baseline] (when it belongs to that suite) or the oldest run is the
         * baseline, and every other run is compared against it.
         *
         * @throws IllegalArgumentException if [runs] is empty.
         */
        public fun of(
            runs: List<RunResult>,
            baseline: RunResult? = null,
        ): Report {
            require(runs.isNotEmpty()) { "No result files to report on" }
            val sorted = runs.sortedWith(compareBy({ it.suite }, { it.startedAt }))
            val diffs =
                sorted.groupBy { it.suite }.values.flatMap { suiteRuns ->
                    val base = suiteRuns.firstOrNull { it === baseline } ?: suiteRuns.first()
                    suiteRuns.filter { it !== base }.map { RunDiff.of(base, it) }
                }
            return Report(sorted, diffs)
        }
    }
}

internal fun label(run: RunResult): String {
    val env = run.environment
    return "${run.suite} · ${env.platform} ${env.modelVersion ?: "unknown model"} · prompt ${run.promptVersion}"
}

internal fun Status.symbol(): String =
    when (this) {
        Status.Passed -> "pass"
        Status.Failed -> "fail"
        Status.NotGraded -> "not graded"
    }

internal fun Double?.percent(): String = if (this == null) "–" else "${(this * 100).toInt()}%"

internal fun Double?.signed(): String =
    when {
        this == null -> "–"
        this > 0 -> "+${(this * 100).toInt()} pts"
        else -> "${(this * 100).toInt()} pts"
    }

internal fun Double?.twoDecimals(): String = if (this == null) "–" else (kotlin.math.round(this * 100) / 100).toString()
