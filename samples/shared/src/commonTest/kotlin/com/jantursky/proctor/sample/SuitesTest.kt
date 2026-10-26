package com.jantursky.proctor.sample

import com.jantursky.proctor.EvalRunner
import com.jantursky.proctor.FakeBackend
import com.jantursky.proctor.RunnerConfig
import com.jantursky.proctor.Status
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

private val quick = RunnerConfig(requestDelay = 0.seconds)

/** Answers the way a perfect model would, by looking up the case the prompt contains. */
private fun oracle(prompt: String): String {
    val answers =
        classifierSuite.cases.associate { it.input to it.expected!! } +
            extractionSuite.cases.associate { it.input to it.expected!! }
    return answers.entries.first { prompt.contains(it.key) }.value
}

class SuitesTest {
    @Test
    fun aPerfectModelPassesBothSuitesOnEveryPromptVersion() =
        runTest {
            val runner = EvalRunner(FakeBackend { prompt, _ -> oracle(prompt) }, quick)
            val results = sampleSuites.flatMap { runner.runAll(it) }

            assertEquals(listOf("v1", "v2", "v1"), results.map { it.promptVersion })
            assertEquals(listOf(true, true, true), results.map { it.summary.suitePassed })
            assertEquals(listOf(12, 12, 4), results.map { it.summary.passed })
        }

    @Test
    fun aChattyModelFailsTheStrictClassifier() =
        runTest {
            val chatty = FakeBackend { _, _ -> "I think this is a Billing issue." }
            val result = EvalRunner(chatty, quick).run(classifierSuite)
            assertEquals(false, result.summary.suitePassed)
            assertEquals(0, result.summary.passed)
        }

    @Test
    fun theExtractionGraderScoresPartialMatches() =
        runTest {
            val wrongUrgency = """{"orderId":"48213","product":"Blender","urgent":false}"""
            val result = EvalRunner(FakeBackend { _, _ -> wrongUrgency }, quick).run(extractionSuite)
            val blender = result.cases.first { it.id == "blender" }

            assertEquals(Status.Failed, blender.status)
            val detail =
                blender.attempts
                    .first()
                    .graders
                    .first { it.name == "detailsMatch" }
            assertEquals("urgent false instead of true", detail.rationale)
            assertEquals(2.0 / 3.0, detail.score)
            assertEquals(
                Status.Passed,
                blender.attempts
                    .first()
                    .graders
                    .first { it.name == "jsonSchema" }
                    .status,
            )
        }

    @Test
    fun detailsGraderHandlesMissingAndUnreadableInput() =
        runTest {
            val noJson = EvalRunner(FakeBackend { _, _ -> "no idea" }, quick).run(extractionSuite)
            val grader =
                noJson.cases
                    .first()
                    .attempts
                    .first()
                    .graders
                    .first { it.name == "detailsMatch" }
            assertEquals("No readable details in the output", grader.rationale)

            val product =
                EvalRunner(FakeBackend { _, _ -> """{"orderId":null,"product":"Toaster","urgent":false}""" }, quick)
                    .run(extractionSuite)
            val shipping =
                product.cases.first { it.id == "shipping" }.attempts.first().graders.first {
                    it.name ==
                        "detailsMatch"
                }
            assertEquals("product Toaster instead of null", shipping.rationale)
        }

    @Test
    fun triageParsesCategoriesAndDetails() =
        runTest {
            val triage =
                TicketTriage { prompt ->
                    if (prompt.contains(
                            "Category:",
                        )
                    ) {
                        "**Billing**."
                    } else {
                        """Sure: {"orderId":"1","product":null,"urgent":true}"""
                    }
                }

            assertEquals(Category.Billing, triage.classify("charged twice"))
            assertEquals(TicketDetails("1", null, true), triage.details("anything"))
            assertNull(TicketTriage { "nothing" }.details("x"))
            assertNull(TicketTriage { "Maybe later" }.classify("x"))
        }

    @Test
    fun parseCategoryAcceptsPunctuationAndCase() {
        assertEquals(Category.Bug, parseCategory("  bug \n"))
        assertEquals(Category.Account, parseCategory("`Account` is the answer"))
        assertNull(parseCategory(""))
    }
}
