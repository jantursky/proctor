package com.jantursky.proctor.gradle

import com.jantursky.proctor.Attempt
import com.jantursky.proctor.AvailabilityState
import com.jantursky.proctor.CaseResult
import com.jantursky.proctor.Environment
import com.jantursky.proctor.RunResult
import com.jantursky.proctor.Sampling
import com.jantursky.proctor.Status
import com.jantursky.proctor.Summary
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProctorPluginTest {
    private fun result(
        status: Status,
        modelVersion: String,
        startedAt: String,
    ): RunResult {
        val case = CaseResult("case-1", "in", "A", status, 1.0, 0.0, listOf(Attempt("A", 1, status, 1.0)))
        return RunResult(
            suite = "tickets",
            promptVersion = "v1",
            environment = Environment("android", "Galaxy S26", "16", modelVersion, "mlkit"),
            sampling = Sampling(),
            repeat = 1,
            startedAt = startedAt,
            availability = AvailabilityState.Available,
            summary = Summary(1, 0, 0, 0, 1.0, suitePassed = status == Status.Passed),
            cases = listOf(case),
        )
    }

    private fun project(
        vararg runs: RunResult,
        extra: String = "",
    ): File {
        val dir = Files.createTempDirectory("proctor-plugin-test").toFile()
        File(dir, "settings.gradle.kts").writeText("rootProject.name = \"consumer\"\n")
        File(dir, "build.gradle.kts").writeText("plugins { id(\"io.github.jantursky.proctor\") }\n$extra\n")
        val results = File(dir, "proctor-results").apply { mkdirs() }
        runs.forEach { File(results, it.fileName()).writeText(it.toJson()) }
        return dir
    }

    private fun gradle(dir: File) =
        GradleRunner
            .create()
            .withProjectDir(dir)
            .withPluginClasspath()
            .withArguments("proctorReport", "--stacktrace")

    @Test
    fun writesTheReportWhenResultsPass() {
        val dir =
            project(
                result(Status.Passed, "nano-v3", "2026-10-26T10:00:00Z"),
                result(Status.Passed, "nano-v4-fast", "2026-10-26T11:00:00Z"),
            )
        val build = gradle(dir).build()

        assertEquals(org.gradle.testkit.runner.TaskOutcome.SUCCESS, build.task(":proctorReport")!!.outcome)
        val out = File(dir, "build/reports/proctor")
        assertTrue(File(out, "index.html").readText().contains("nano-v4-fast"))
        assertTrue(File(out, "report.md").readText().contains("model version: nano-v3 to nano-v4-fast"))
    }

    @Test
    fun failsTheBuildWhenAResultFailsItsCondition() {
        val dir = project(result(Status.Failed, "nano-v3", "2026-10-26T10:00:00Z"))
        val build = gradle(dir).buildAndFail()

        assertTrue(build.output.contains("do not meet the pass conditions"))
        assertTrue(File(dir, "build/reports/proctor/index.html").exists())
    }

    @Test
    fun honoursTheExtension() {
        val older = result(Status.Passed, "nano-v3", "2026-10-26T10:00:00Z")
        val newer = result(Status.Passed, "nano-v4-fast", "2026-10-26T11:00:00Z")
        val settings = "baseline.set(\"${newer.fileName()}\"); outputDir.set(layout.projectDirectory.dir(\"out\"))"
        val dir = project(older, newer, extra = "proctor { $settings }")
        gradle(dir).build()

        assertTrue(File(dir, "out/report.md").readText().contains("Base tickets · android nano-v4-fast"))
    }
}
