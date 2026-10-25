package com.jantursky.proctor.android

import com.jantursky.proctor.AvailabilityState
import com.jantursky.proctor.BackendAvailability
import com.jantursky.proctor.BackendException
import com.jantursky.proctor.EvalRunner
import com.jantursky.proctor.FakeBackend
import com.jantursky.proctor.RunAbortedException
import com.jantursky.proctor.RunResult
import com.jantursky.proctor.RunnerConfig
import com.jantursky.proctor.Sampling
import com.jantursky.proctor.evalSuite
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ForegroundAndStoreTest {
    private val suite =
        evalSuite("s") {
            prompt("v1") { it }
            case("a") { expect("A") }
            case("b") { expect("A") }
            graders { exactMatch() }
        }

    @Test
    fun delegatesWhileTheAppIsInTheForeground() =
        runTest {
            val fake = FakeBackend(BackendAvailability.Downloading) { _, _ -> "A" }
            val guarded = ForegroundBackend(fake) { true }

            assertEquals(BackendAvailability.Downloading, guarded.availability())
            assertEquals("fake", guarded.environment().platform)
            assertEquals("A", guarded.generate("x", Sampling()))
        }

    @Test
    fun aLostForegroundAbortsTheRunBeforeTheModelIsCalled() =
        runTest {
            var foreground = true
            val fake =
                FakeBackend { _, _ ->
                    foreground = false
                    "A"
                }
            val runner = EvalRunner(ForegroundBackend(fake) { foreground }, RunnerConfig(requestDelay = 0.seconds))

            assertFailsWith<RunAbortedException> { runner.run(suite) }
            assertEquals(1, fake.prompts.size)
            val background = ForegroundBackend(fake) { false }
            assertFailsWith<BackendException.Backgrounded> { background.generate("x", Sampling()) }
        }

    @Test
    fun theHostActivityIsNotForegroundUntilItResumes() {
        assertEquals(false, ProctorHostActivity.isForeground())
    }

    @Test
    fun resultsAreWrittenUnderTheirFileNameAndReadBack() =
        runTest {
            val dir = Files.createTempDirectory("proctor-store").resolve("proctor").toFile()
            val result = EvalRunner(FakeBackend { _, _ -> "A" }, RunnerConfig(requestDelay = 0.seconds)).run(suite)
            val file = ResultStore(dir).write(result)

            assertEquals(result.fileName(), file.name)
            assertEquals(result, RunResult.fromJson(file.readText()))
            assertEquals(AvailabilityState.Available, RunResult.fromJson(file.readText()).availability)
            assertTrue(file.name.endsWith(".proctor.json"))
        }
}
