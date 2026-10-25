@file:OptIn(ExperimentalCoroutinesApi::class)

package com.jantursky.proctor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

private val quickRun = RunnerConfig(requestDelay = 0.seconds)

private fun threeCases() = classifier("a" to "A", "b" to "A", "c" to "A")

private fun classifier(
    vararg cases: Pair<String, String>,
    block: EvalSuiteBuilder.() -> Unit = {},
) = evalSuite("ticket-classifier") {
    prompt("v1") { "Classify: $it" }
    cases.forEach { (input, expected) -> case(input) { expect(expected) } }
    graders { exactMatch() }
    block()
}

class RunnerTest {
    @Test
    fun gradesEveryCaseAndSummarizes() =
        runTest {
            val backend = FakeBackend { prompt, _ -> if (prompt.endsWith("card")) "Billing" else "Wrong" }
            val result = EvalRunner(backend, quickRun).run(classifier("card" to "Billing", "crash" to "Bug"))

            assertEquals(listOf(Status.Passed, Status.Failed), result.cases.map { it.status })
            assertEquals(Summary(2, 1, 1, 0, 0.5, suitePassed = false), result.summary)
            assertEquals(listOf("Classify: card", "Classify: crash"), backend.prompts)
            assertEquals("fake-model-1", result.environment.modelVersion)
            assertEquals("v1", result.promptVersion)
            assertEquals(AvailabilityState.Available, result.availability)
            val rationale =
                result.cases[1]
                    .attempts
                    .single()
                    .graders
                    .single()
                    .rationale
            assertEquals("Expected \"Bug\", got \"Wrong\"", rationale)
        }

    @Test
    fun repeatedRunsReportMeanAndSpread() =
        runTest {
            val backend = FakeBackend { _, call -> if (call % 2 == 0) "A" else "B" }
            val result = EvalRunner(backend, quickRun).run(classifier("x" to "A") { repeat(4) })
            val case = result.cases.single()

            assertEquals(4, case.attempts.size)
            assertEquals(0.5, case.meanScore)
            assertEquals(0.5, case.spread)
            assertEquals(Status.Failed, case.status)
            assertEquals(4, result.repeat)
        }

    @Test
    fun scoresAveragedAcrossGradersAndNotGradedGradersAreSkipped() =
        runTest {
            val failing =
                object : Grader {
                    override val name = "boom"

                    override suspend fun grade(context: GradeContext): Grade = error("grader crashed")
                }
            val suite = classifier("x" to "A") { graders { add(failing) } }
            val result = EvalRunner(FakeBackend { _, _ -> "A" }, quickRun).run(suite)
            val attempt =
                result.cases
                    .single()
                    .attempts
                    .single()

            assertEquals(Status.Passed, attempt.status)
            assertEquals(Status.NotGraded, attempt.graders.last().status)
            assertEquals("Grader failed: grader crashed", attempt.graders.last().rationale)
        }

    @Test
    fun anAttemptWhereNoGraderCouldJudgeIsNotGraded() =
        runTest {
            val suite =
                evalSuite("s") {
                    prompt("v") { it }
                    case("no expected value")
                    graders { exactMatch() }
                }
            val result = EvalRunner(FakeBackend { _, _ -> "x" }, quickRun).run(suite)
            assertEquals(Status.NotGraded, result.cases.single().status)
            assertNull(result.cases.single().meanScore)
            assertNull(result.summary.meanScore)
            assertEquals(false, result.summary.suitePassed)
        }

    @Test
    fun retriesAfterQuotaErrorsWithBackoffThenSucceeds() =
        runTest {
            val backend =
                FakeBackend { _, call ->
                    if (call < 2) throw BackendException.QuotaExceeded() else "A"
                }
            val config = RunnerConfig(requestDelay = 0.seconds, initialBackoff = 5.seconds)
            val result = EvalRunner(backend, config).run(classifier("x" to "A"))

            assertEquals(Status.Passed, result.cases.single().status)
            assertEquals(3, backend.prompts.size)
            assertEquals(15_000, currentTime) // 5s then 10s
        }

    @Test
    fun usesTheQuotaRetryHintAndCapsTheBackoff() =
        runTest {
            val backend =
                FakeBackend { _, call ->
                    when (call) {
                        0 -> throw BackendException.QuotaExceeded(retryAfter = 7.seconds)
                        1, 2 -> throw BackendException.QuotaExceeded()
                        else -> "A"
                    }
                }
            val config = RunnerConfig(requestDelay = 0.seconds, initialBackoff = 5.seconds, maxBackoff = 6.seconds)
            EvalRunner(backend, config).run(classifier("x" to "A"))
            assertEquals(7_000 + 6_000 + 6_000, currentTime)
        }

    @Test
    fun quotaThatNeverClearsIsNotGradedNotFailed() =
        runTest {
            val backend = FakeBackend { _, _ -> throw BackendException.QuotaExceeded() }
            val config = RunnerConfig(requestDelay = 0.seconds, maxQuotaRetries = 2)
            val result = EvalRunner(backend, config).run(classifier("x" to "A"))
            val attempt =
                result.cases
                    .single()
                    .attempts
                    .single()

            assertEquals(Status.NotGraded, result.cases.single().status)
            assertEquals(3, backend.prompts.size)
            assertTrue(attempt.note!!.contains("quota"))
            assertEquals(0, result.summary.failed)
            assertEquals(1, result.summary.notGraded)
        }

    @Test
    fun leavingTheForegroundAbortsTheRun() =
        runTest {
            val backend = FakeBackend { _, call -> if (call == 1) throw BackendException.Backgrounded() else "A" }
            val error =
                assertFailsWith<RunAbortedException> {
                    EvalRunner(backend, quickRun).run(threeCases())
                }
            assertTrue(error.message!!.contains("foreground"))
        }

    @Test
    fun backendErrorsAreNotGradedWithTheirReason() =
        runTest {
            val backend =
                FakeBackend { _, call ->
                    when (call) {
                        0 -> throw BackendException.Failed("safety block")
                        1 -> throw BackendException.Unavailable("config missing")
                        else -> "A"
                    }
                }
            val result = EvalRunner(backend, quickRun).run(threeCases())
            assertEquals(listOf(Status.NotGraded, Status.NotGraded, Status.Passed), result.cases.map { it.status })
            assertEquals(
                "Backend error: safety block",
                result.cases[0]
                    .attempts
                    .single()
                    .note,
            )
            assertEquals(
                "Model unavailable: config missing",
                result.cases[1]
                    .attempts
                    .single()
                    .note,
            )
        }

    @Test
    fun unavailableModelsAreReportedInsteadOfFailingCases() =
        runTest {
            val states =
                mapOf(
                    BackendAvailability.Downloadable to AvailabilityState.Downloadable,
                    BackendAvailability.Downloading to AvailabilityState.Downloading,
                    BackendAvailability.Unavailable("bootloader unlocked") to AvailabilityState.Unavailable,
                )
            for ((availability, state) in states) {
                val backend = FakeBackend(availability) { _, _ -> error("must not be called") }
                val result = EvalRunner(backend, quickRun).run(classifier("a" to "A", "b" to "A"))

                assertEquals(state, result.availability)
                assertEquals(2, result.summary.notGraded)
                assertEquals(0, result.summary.failed)
                assertTrue(backend.prompts.isEmpty())
                assertTrue(
                    result.cases
                        .first()
                        .attempts
                        .single()
                        .note!!
                        .startsWith("Model not available"),
                )
            }
            val unavailable =
                EvalRunner(FakeBackend(BackendAvailability.Unavailable("x")) { _, _ -> "" }, quickRun)
                    .run(classifier("a" to "A"))
            assertEquals("x", unavailable.availabilityReason)
            assertTrue(
                unavailable.cases
                    .single()
                    .attempts
                    .single()
                    .note!!
                    .contains("unavailable (x)"),
            )
        }

    @Test
    fun pacesRequestsWithTheConfiguredDelay() =
        runTest {
            val backend = FakeBackend { _, _ -> "A" }
            val config = RunnerConfig(requestDelay = 2.seconds)
            EvalRunner(backend, config).run(threeCases())
            assertEquals(6_000, currentTime)
        }

    @Test
    fun concurrencyKeepsCaseOrderAndSharesTheDelay() =
        runTest {
            val backend = FakeBackend { prompt, _ -> prompt.removePrefix("Classify: ").uppercase() }
            val suite = classifier("a" to "A", "b" to "B", "c" to "C", "d" to "D")
            val result = EvalRunner(backend, RunnerConfig(concurrency = 2, requestDelay = 2.seconds)).run(suite)

            assertEquals(listOf("case-1", "case-2", "case-3", "case-4"), result.cases.map { it.id })
            assertTrue(result.cases.all { it.status == Status.Passed })
            assertEquals(4_000, currentTime)
        }

    @Test
    fun runAllProducesOneResultPerPromptVersion() =
        runTest {
            val suite = classifier("a" to "A") { prompt("v2") { "Label: $it" } }
            val backend = FakeBackend { prompt, _ -> if (prompt.startsWith("Label")) "A" else "B" }
            val results = EvalRunner(backend, quickRun).runAll(suite)

            assertEquals(listOf("v1", "v2"), results.map { it.promptVersion })
            assertEquals(listOf(Status.Failed, Status.Passed), results.map { it.cases.single().status })
        }

    @Test
    fun recordsTheClockTimeAndSampling() =
        runTest {
            val clock =
                object : kotlin.time.Clock {
                    override fun now() = kotlin.time.Instant.fromEpochSeconds(0)
                }
            val sampling = Sampling(temperature = 0.0, topK = 1)
            val result =
                EvalRunner(FakeBackend { _, _ -> "A" }, quickRun, clock)
                    .run(classifier("a" to "A") { sampling(sampling) })
            assertEquals("1970-01-01T00:00:00Z", result.startedAt)
            assertEquals(sampling, result.sampling)
        }

    @Test
    fun customPassVerdictDecidesTheSuite() =
        runTest {
            val suite = classifier("a" to "A", "b" to "A") { passWhen { score >= 0.5 } }
            val result = EvalRunner(FakeBackend { _, call -> if (call == 0) "A" else "B" }, quickRun).run(suite)
            assertTrue(result.summary.suitePassed)
        }

    @Test
    fun configRejectsInvalidValues() {
        assertFailsWithMessage<IllegalArgumentException>("concurrency") { RunnerConfig(concurrency = 0) }
        assertFailsWithMessage<IllegalArgumentException>("maxQuotaRetries") { RunnerConfig(maxQuotaRetries = -1) }
    }

    @Test
    fun fakeBackendRecordsPromptsEvenWhenTheyThrow() =
        runTest {
            val backend = FakeBackend { _, _ -> throw BackendException.Failed("x") }
            assertFailsWith<BackendException.Failed> { backend.generate("p", Sampling()) }
            assertEquals(listOf("p"), backend.prompts)
        }
}
