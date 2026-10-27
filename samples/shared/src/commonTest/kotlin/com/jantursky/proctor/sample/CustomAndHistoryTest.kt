package com.jantursky.proctor.sample

import com.jantursky.proctor.EvalRunner
import com.jantursky.proctor.FakeBackend
import com.jantursky.proctor.RunnerConfig
import com.jantursky.proctor.Status
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class CustomAndHistoryTest {
    @Test
    fun aCustomCaseRunsWithTheSuitesPromptsAndGraders() =
        runTest {
            val backend = FakeBackend { _, _ -> "Billing" }
            val case = runCustomCase(backend, "ticket-classifier", "v2", "I was charged twice", "billing")
            assertNotNull(case)
            assertEquals("custom", case.id)
            assertEquals(Status.Passed, case.status)
            assertEquals(3, case.attempts.size, "the suite's repeat count applies")
            assertEquals(true, backend.prompts.first().contains("I was charged twice"))
        }

    @Test
    fun aWrongAnswerFailsTheCustomCase() =
        runTest {
            val case = runCustomCase(FakeBackend { _, _ -> "Bug" }, "ticket-classifier", "v1", "Hello", "Other")
            assertEquals(Status.Failed, case?.status)
        }

    @Test
    fun withoutAnExpectedValueTheCaseCannotBeGraded() =
        runTest {
            val case = runCustomCase(FakeBackend { _, _ -> "Bug" }, "ticket-classifier", "v1", "Hello", "  ")
            assertEquals(Status.NotGraded, case?.status)
        }

    @Test
    fun anUnknownSuiteGivesNothing() =
        runTest {
            assertNull(runCustomCase(FakeBackend { _, _ -> "x" }, "nope", "v1", "a", "b"))
        }

    @Test
    fun runsStartedTogetherShareAGroupAndTheNewestGroupComesFirst() =
        runTest {
            val older = EvalRunner(FakeBackend { _, _ -> "x" }, RunnerConfig(requestDelay = 0.seconds, groupId = "old"))
            val newer = EvalRunner(FakeBackend { _, _ -> "x" }, RunnerConfig(requestDelay = 0.seconds, groupId = "new"))
            val rows =
                (older.runAll(classifierSuite) + newer.runAll(classifierSuite) + older.runAll(extractionSuite))
                    .map { it.toSampleRow() }

            val groups = groupHistory(rows)
            assertEquals(
                listOf("new", "old"),
                groups.map { it.id }.let { ids ->
                    ids.sortedBy {
                        if (it ==
                            "new"
                        ) {
                            0
                        } else {
                            1
                        }
                    }
                },
            )
            assertEquals(2, groups.single { it.id == "new" }.rows.size)
            assertEquals(3, groups.single { it.id == "old" }.rows.size)
            assertEquals(groups.sortedByDescending { it.startedAt }.map { it.id }, groups.map { it.id })
        }

    @Test
    fun runsWithoutAGroupIdLandInOneBucket() =
        runTest {
            val runner = EvalRunner(FakeBackend { _, _ -> "x" }, RunnerConfig(requestDelay = 0.seconds))
            val groups = groupHistory(runner.runAll(classifierSuite).map { it.toSampleRow() })
            assertEquals(1, groups.size)
            assertNull(groups.single().id)
        }

    @Test
    fun aBrokenFileIsSkippedWhenReadingHistory() {
        assertNull(parseSampleRow("not json"))
    }
}
