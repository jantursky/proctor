package com.jantursky.proctor.report

import com.jantursky.proctor.Status.Failed
import com.jantursky.proctor.Status.Passed
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportJobTest {
    private val v3 = run(case("a", Passed), case("b", Failed), suitePassed = true)
    private val v4 =
        run(case("a", Passed), case("b", Passed), modelVersion = "nano-v4-fast", startedAt = "2026-10-26T11:00:00Z")

    private fun results(vararg runs: com.jantursky.proctor.RunResult): Path {
        val dir = Files.createTempDirectory("proctor-job-results")
        runs.forEach { dir.resolve(it.fileName()).writeText(it.toJson()) }
        return dir
    }

    @Test
    fun writesBothReportsAndReturnsTheVerdict() {
        val out = Files.createTempDirectory("proctor-job-out").resolve("nested")
        val result = ReportJob.run(results(v3, v4), out)

        assertEquals(out.resolve("index.html"), result.html)
        assertTrue(result.html.readText().contains("<h1>Proctor report</h1>"))
        assertTrue(result.markdown.readText().startsWith("# Proctor report"))
        assertTrue(result.verdict.passed)
    }

    @Test
    fun comparesAgainstTheNamedBaseline() {
        val out = Files.createTempDirectory("proctor-job-out")
        val dir = results(v3, v4)
        val byPath = ReportJob.run(dir, out, baseline = dir.resolve(v4.fileName()).toString())
        assertTrue("Base tickets · android nano-v4-fast" in byPath.markdown.readText())
        val byName = ReportJob.run(dir, out, baseline = v4.fileName())
        assertTrue("Base tickets · android nano-v4-fast" in byName.markdown.readText())
    }

    @Test
    fun failsTheVerdictWhenARunFailsItsCondition() {
        val out = Files.createTempDirectory("proctor-job-out")
        val result = ReportJob.run(results(run(case("a", Failed))), out, options = VerdictOptions())
        assertFalse(result.verdict.passed)
    }

    @Test
    fun explainsMissingInputs() {
        val out = Files.createTempDirectory("proctor-job-out")
        assertTrue(
            assertFailsWith<IllegalArgumentException> {
                ReportJob.run(results(), out)
            }.message!!.startsWith("No *.proctor.json"),
        )
        assertTrue(
            assertFailsWith<IllegalArgumentException> { ReportJob.run(results(v3), out, baseline = "nope.json") }
                .message!!
                .startsWith("No result file named nope.json"),
        )
    }
}
