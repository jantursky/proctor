package com.jantursky.proctor.report

import com.jantursky.proctor.Status.Failed
import com.jantursky.proctor.Status.NotGraded
import com.jantursky.proctor.Status.Passed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RunDiffTest {
    @Test
    fun classifiesEveryCaseMovement() {
        val base =
            run(
                case("same", Passed),
                case("worse", Passed),
                case("better", Failed),
                case("ungraded", Passed),
                case("both-ungraded", NotGraded),
                case("gone", Passed),
            )
        val other =
            run(
                case("same", Passed),
                case("worse", Failed),
                case("better", Passed),
                case("ungraded", NotGraded),
                case("both-ungraded", NotGraded),
                case("new", Passed),
            )
        val diff = RunDiff.of(base, other)

        assertEquals(
            mapOf(
                "same" to Change.Same,
                "worse" to Change.Regressed,
                "better" to Change.Improved,
                "ungraded" to Change.NotComparable,
                "both-ungraded" to Change.NotComparable,
                "gone" to Change.OnlyInBase,
                "new" to Change.OnlyInOther,
            ),
            diff.cases.associate { it.id to it.change },
        )
        assertEquals(1, diff.regressions)
        assertEquals(1, diff.improvements)
        assertEquals(base.cases.map { it.input } + "input new", diff.cases.map { it.input })
    }

    @Test
    fun namesWhatDiffersBetweenTheRuns() {
        val base = run(case("a", Passed))
        val other = run(case("a", Passed), modelVersion = "nano-v4-fast", platform = "ios", prompt = "v2")
        val diff = RunDiff.of(base, other)

        assertEquals(listOf(DiffAxis.Platform, DiffAxis.ModelVersion, DiffAxis.PromptVersion), diff.axes)
        assertEquals(
            "platform: android to ios; model version: nano-v3 to nano-v4-fast; prompt version: v1 to v2",
            diff.describe(),
        )
        assertEquals("same configuration, different run", RunDiff.of(base, base).describe())
    }

    @Test
    fun unknownModelVersionIsNamedUnknown() {
        val diff = RunDiff.of(run(case("a", Passed)), run(case("a", Passed), modelVersion = null))
        assertEquals("model version: nano-v3 to unknown", diff.describe())
    }

    @Test
    fun scoreDeltasNeedScoresOnBothSides() {
        val diff =
            RunDiff.of(
                run(case("a", Passed, score = 1.0), case("b", Failed, score = 0.0)),
                run(case("a", Passed, score = 0.5), case("b", NotGraded)),
            )
        assertEquals(-0.5, diff.cases[0].scoreDelta)
        assertNull(diff.cases[1].scoreDelta)
        assertEquals(0.5, diff.base.summary.meanScore)
        assertEquals(0.5, diff.other.summary.meanScore)
        assertEquals(0.0, diff.scoreDelta)
        assertNull(RunDiff.of(run(case("a", NotGraded)), run(case("a", Passed))).scoreDelta)
        assertNull(RunDiff.of(run(case("a", Passed)), run(case("a", NotGraded))).scoreDelta)
        assertNull(
            RunDiff
                .of(run(case("a", Passed), case("x", Passed)), run(case("b", Passed)))
                .cases
                .last()
                .scoreDelta,
        )
    }

    @Test
    fun refusesToCompareDifferentSuites() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                RunDiff.of(run(case("a", Passed)), run(case("a", Passed), suite = "other"))
            }
        assertEquals("Cannot compare suite \"tickets\" with \"other\"", error.message)
    }
}
