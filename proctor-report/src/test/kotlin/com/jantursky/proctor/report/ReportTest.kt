package com.jantursky.proctor.report

import com.jantursky.proctor.AvailabilityState
import com.jantursky.proctor.Status.Failed
import com.jantursky.proctor.Status.NotGraded
import com.jantursky.proctor.Status.Passed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ReportTest {
    private val v3 = run(case("a", Passed), case("b", Failed), startedAt = "2026-10-26T10:00:00Z", suitePassed = true)
    private val v4 =
        run(
            case("a", Passed),
            case("b", Passed),
            modelVersion = "nano-v4-fast",
            startedAt = "2026-10-26T11:00:00Z",
        )

    @Test
    fun comparesEveryRunAgainstTheOldestOfItsSuite() {
        val other = run(case("x", Passed), suite = "other")
        val report = Report.of(listOf(v4, other, v3))

        assertEquals(listOf(other, v3, v4), report.runs)
        assertEquals(1, report.diffs.size)
        assertSame(v3, report.diffs.single().base)
        assertSame(v4, report.diffs.single().other)
    }

    @Test
    fun anExplicitBaselineWinsWhenItBelongsToTheSuite() {
        val report = Report.of(listOf(v3, v4), baseline = v4)
        assertSame(v4, report.diffs.single().base)
        assertSame(v3, report.diffs.single().other)

        val stranger = run(case("a", Passed), suite = "elsewhere")
        assertSame(
            v3,
            Report
                .of(listOf(v3, v4), baseline = stranger)
                .diffs
                .single()
                .base,
        )
    }

    @Test
    fun aSingleRunHasNoDiffsAndNoRunsIsRefused() {
        assertTrue(Report.of(listOf(v3)).diffs.isEmpty())
        assertFailsWith<IllegalArgumentException> { Report.of(emptyList()) }
    }

    @Test
    fun markdownListsEnvironmentResultsDiffAndCases() {
        val md = Report.of(listOf(v3, v4)).toMarkdown()

        assertTrue("| tickets | android | Galaxy S26 | 16 | nano-v3 | mlkit | v1 | available |" in md)
        assertTrue("nano-v4-fast" in md)
        assertTrue("Differs in model version: nano-v3 to nano-v4-fast." in md)
        assertTrue("0 regressed, 1 improved" in md)
        assertTrue("(+50 pts)" in md)
        assertTrue("| b | fail | pass | Improved |" in md)
        assertTrue("## Cases: tickets · android nano-v4-fast · prompt v1" in md)
    }

    @Test
    fun markdownKeepsTablesIntactForAwkwardOutput() {
        val awkward = run(case("a", Passed, output = "line one\nline | two " + "x".repeat(300)))
        val md = Report.of(listOf(awkward)).toMarkdown()
        val row = md.lines().first { it.startsWith("| a | input a") }

        assertTrue("line one line \\| two" in row)
        assertTrue(row.contains("…"))
        assertFalse(md.contains("line one\nline"))
    }

    @Test
    fun markdownShowsNotGradedReasonsAndAvailability() {
        val ungraded = run(case("q", NotGraded, output = null), availability = AvailabilityState.Unavailable)
        val md = Report.of(listOf(ungraded)).toMarkdown()

        assertTrue("unavailable (bootloader)" in md)
        assertTrue("(quota)" in md)
        assertTrue("Not graded cases are not prompt failures: q." in md)
        assertTrue("| – |" in md || "–" in md)
    }

    @Test
    fun markdownShowsCasesAbsentFromOneSide() {
        val md = Report.of(listOf(run(case("a", Passed), case("old", Passed)), v4.copy())).toMarkdown()
        assertTrue("| old | pass | – | OnlyInBase |" in md)
    }

    @Test
    fun htmlIsOneEscapedSelfContainedPage() {
        val nasty = run(case("a", Passed, output = "<script>alert('x')</script> & \"q\"", input = "<b>in</b>"))
        val html = Report.of(listOf(nasty, v4)).toHtml()

        assertTrue(html.startsWith("<!doctype html>"))
        assertFalse("<script>" in html)
        assertTrue("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; &quot;q&quot;" in html)
        assertTrue("&lt;b&gt;in&lt;/b&gt;" in html)
        assertFalse("http://" in html || "https://" in html)
        assertTrue("prefers-color-scheme:dark" in html)
        assertTrue("class=\"tag pass\"" in html)
    }

    @Test
    fun htmlCoversDiffAttemptsAndNotGradedNotes() {
        val ungraded = run(case("q", NotGraded, output = null), case("old", Passed), startedAt = "2026-10-26T09:00:00Z")
        val html = Report.of(listOf(ungraded, v3)).toHtml()

        assertTrue("class=\"tag ng\"" in html)
        assertTrue("class=\"tag fail\"" in html)
        assertTrue("<p class=\"muted\">quota</p>" in html)
        assertTrue("OnlyInBase" in html)
        assertTrue("Compare: tickets" in html)
        assertTrue("Expected <span class=\"mono\">A</span>" in html)
        assertTrue("Not graded means no verdict" in html)
    }

    @Test
    fun htmlShowsUnknownModelVersionAndAvailabilityReason() {
        val html =
            Report
                .of(
                    listOf(run(case("a", Passed), modelVersion = null, availability = AvailabilityState.Unavailable)),
                ).toHtml()
        assertTrue("<td class=\"mono\">unknown</td>" in html)
        assertTrue("unavailable (bootloader)" in html)
    }

    @Test
    fun verdictFailsWhenARunMissesItsOwnCondition() {
        val failing = run(case("a", Failed))
        val verdict = Report.of(listOf(failing)).verdict()

        assertFalse(verdict.passed)
        assertEquals(listOf("tickets · android nano-v3 · prompt v1 did not meet its pass condition"), verdict.reasons)
        assertTrue(Report.of(listOf(v3, v4)).verdict().passed)
    }

    @Test
    fun verdictOptionsAddRegressionAndNotGradedChecks() {
        val worse = run(case("a", Passed), case("b", Failed), suitePassed = true, startedAt = "2026-10-26T12:00:00Z")
        val better = run(case("a", Failed), case("b", Passed), suitePassed = true, startedAt = "2026-10-26T13:00:00Z")
        val ungraded = run(case("a", NotGraded), suitePassed = true, startedAt = "2026-10-26T14:00:00Z")
        val report = Report.of(listOf(worse, better, ungraded))

        assertTrue(report.verdict().passed)
        val regression = report.verdict(VerdictOptions(failOnRegression = true))
        assertFalse(regression.passed)
        assertEquals(1, regression.reasons.count { "regressed on 1 case(s)" in it })
        val notGraded = report.verdict(VerdictOptions(failOnNotGraded = true))
        assertEquals(listOf("tickets · android nano-v3 · prompt v1 has 1 case(s) not graded"), notGraded.reasons)
    }

    @Test
    fun formattingHelpers() {
        assertEquals("–", null.percent())
        assertEquals("75%", 0.75.percent())
        assertEquals("–", null.signed())
        assertEquals("+25 pts", 0.25.signed())
        assertEquals("-25 pts", (-0.25).signed())
        assertEquals("0 pts", 0.0.signed())
        assertEquals("–", null.twoDecimals())
        assertEquals("0.33", (1.0 / 3).twoDecimals())
        assertEquals("fail", Failed.symbol())
    }
}
