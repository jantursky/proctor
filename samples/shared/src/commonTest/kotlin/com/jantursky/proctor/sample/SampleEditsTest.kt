package com.jantursky.proctor.sample

import com.jantursky.proctor.EvalRunner
import com.jantursky.proctor.FakeBackend
import com.jantursky.proctor.RunnerConfig
import com.jantursky.proctor.Status
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

private val quick = RunnerConfig(requestDelay = 0.seconds)

class SampleEditsTest {
    @Test
    fun withoutEditsTheDatasetIsTheDefaultOne() {
        val edits = SampleEdits()
        assertEquals(sampleSuites.sumOf { it.cases.size }, edits.cases().size)
        assertEquals(0, edits.editedCount)
        assertTrue(edits.cases().none { it.edited })
    }

    @Test
    fun anEditedCaseIsUsedByTheNextRun() =
        runTest {
            val edits = SampleEdits()
            edits.set("ticket-classifier", "office", "Do you have a phone number?", "Other")
            val backend = FakeBackend { _, _ -> "Other" }
            val suite = edits.suites().first { it.name == "ticket-classifier" }
            val result = EvalRunner(backend, quick).run(suite)

            assertTrue(backend.prompts.any { it.contains("Do you have a phone number?") })
            assertTrue(backend.prompts.none { it.contains("Where is your office located?") })
            assertEquals(Status.Passed, result.cases.first { it.id == "office" }.status)
            assertEquals("Do you have a phone number?", result.cases.first { it.id == "office" }.input)
        }

    @Test
    fun theExpectedValueCanChangeToo() =
        runTest {
            val edits = SampleEdits()
            edits.set("ticket-classifier", "thanks", "Thanks for the quick help last week, just saying hi", "Billing")
            val suite = edits.suites().first { it.name == "ticket-classifier" }
            val result = EvalRunner(FakeBackend { _, _ -> "Other" }, quick).run(suite)
            assertEquals(Status.Failed, result.cases.first { it.id == "thanks" }.status)
        }

    @Test
    fun anEmptyExpectedValueMeansNoneAndTheCaseIsNotGraded() =
        runTest {
            val edits = SampleEdits()
            edits.set("ticket-classifier", "office", "Where is your office located?", "  ")
            assertNull(edits.cases().first { it.id == "office" }.expected)
            val suite = edits.suites().first { it.name == "ticket-classifier" }
            val result = EvalRunner(FakeBackend { _, _ -> "Other" }, quick).run(suite)
            assertEquals(Status.NotGraded, result.cases.first { it.id == "office" }.status)
        }

    @Test
    fun savingTheDefaultValuesAgainRemovesTheEdit() {
        val edits = SampleEdits()
        edits.set("ticket-classifier", "office", "changed", "Other")
        assertEquals(1, edits.editedCount)
        edits.set("ticket-classifier", "office", "Where is your office located?", "Other")
        assertEquals(0, edits.editedCount)
    }

    @Test
    fun resetBringsBackTheDefaults() {
        val edits = SampleEdits()
        edits.set("ticket-classifier", "office", "x", "Other")
        edits.set("ticket-extraction", "blender", "y", "{}")
        edits.reset("ticket-classifier", "office")
        assertEquals(1, edits.editedCount)
        edits.resetAll()
        assertEquals(0, edits.editedCount)
    }

    @Test
    fun anUnknownCaseIsIgnored() {
        val edits = SampleEdits()
        edits.set("nope", "x", "a", "b")
        edits.set("ticket-classifier", "nope", "a", "b")
        assertEquals(0, edits.editedCount)
    }

    @Test
    fun theRebuiltSuitesKeepTheirPassRule() =
        runTest {
            val edits = SampleEdits()
            val result = EvalRunner(FakeBackend { _, _ -> "Wrong" }, quick).run(edits.suites().first())
            assertEquals(false, result.summary.suitePassed)
        }
}
