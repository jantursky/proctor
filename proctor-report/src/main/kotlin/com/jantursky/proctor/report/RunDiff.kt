package com.jantursky.proctor.report

import com.jantursky.proctor.CaseResult
import com.jantursky.proctor.RunResult
import com.jantursky.proctor.Status

/** What differs between two runs of the same suite. */
public enum class DiffAxis(
    public val label: String,
) {
    Platform("platform"),
    Device("device"),
    OsVersion("OS version"),
    ModelVersion("model version"),
    Backend("backend"),
    PromptVersion("prompt version"),
}

/** How one case moved from the base run to the other. */
public enum class Change {
    Same,
    Improved,
    Regressed,

    /** One side was not graded, so the pair says nothing about the prompt or the model. */
    NotComparable,
    OnlyInBase,
    OnlyInOther,
}

public class CaseDiff internal constructor(
    public val id: String,
    public val input: String,
    public val base: CaseResult?,
    public val other: CaseResult?,
    public val change: Change,
) {
    /** Other minus base mean score, `null` when either side has no score. */
    public val scoreDelta: Double? get() = other?.meanScore?.let { o -> base?.meanScore?.let { o - it } }
}

/** Two runs of one suite, compared case by case. */
public class RunDiff internal constructor(
    public val base: RunResult,
    public val other: RunResult,
    public val cases: List<CaseDiff>,
) {
    public val axes: List<DiffAxis> = axesOf(base, other)

    public val regressions: Int get() = cases.count { it.change == Change.Regressed }
    public val improvements: Int get() = cases.count { it.change == Change.Improved }

    /** Other minus base mean suite score. */
    public val scoreDelta: Double? get() = other.summary.meanScore?.let { o -> base.summary.meanScore?.let { o - it } }

    /** The differing facts, such as `model version: nano-v3 to nano-v4-fast`. */
    public fun describe(): String =
        if (axes.isEmpty()) {
            "same configuration, different run"
        } else {
            axes.joinToString("; ") { "${it.label}: ${value(base, it)} to ${value(other, it)}" }
        }

    public companion object {
        /** @throws IllegalArgumentException if the runs belong to different suites. */
        public fun of(
            base: RunResult,
            other: RunResult,
        ): RunDiff {
            require(base.suite == other.suite) {
                "Cannot compare suite \"${base.suite}\" with \"${other.suite}\""
            }
            val baseCases = base.cases.associateBy { it.id }
            val otherCases = other.cases.associateBy { it.id }
            val ids = (base.cases.map { it.id } + other.cases.map { it.id }).distinct()
            val cases =
                ids.map { id ->
                    val b = baseCases[id]
                    val o = otherCases[id]
                    CaseDiff(id, b?.input ?: o?.input.orEmpty(), b, o, classify(b, o))
                }
            return RunDiff(base, other, cases)
        }

        private fun classify(
            base: CaseResult?,
            other: CaseResult?,
        ): Change {
            if (base == null) return Change.OnlyInOther
            if (other == null) return Change.OnlyInBase
            return when {
                base.status == other.status && base.status != Status.NotGraded -> Change.Same
                base.status == Status.NotGraded || other.status == Status.NotGraded -> Change.NotComparable
                other.status == Status.Failed -> Change.Regressed
                else -> Change.Improved
            }
        }
    }
}

internal fun value(
    run: RunResult,
    axis: DiffAxis,
): String =
    when (axis) {
        DiffAxis.Platform -> run.environment.platform
        DiffAxis.Device -> run.environment.deviceModel
        DiffAxis.OsVersion -> run.environment.osVersion
        DiffAxis.ModelVersion -> run.environment.modelVersion ?: "unknown"
        DiffAxis.Backend -> run.environment.backend
        DiffAxis.PromptVersion -> run.promptVersion
    }

private fun axesOf(
    base: RunResult,
    other: RunResult,
): List<DiffAxis> = DiffAxis.entries.filter { value(base, it) != value(other, it) }
