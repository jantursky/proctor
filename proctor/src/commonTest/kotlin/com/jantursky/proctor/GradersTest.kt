package com.jantursky.proctor

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Serializable
private data class Ticket(
    val category: String,
    val urgent: Boolean = false,
)

private fun context(
    output: String,
    expected: String? = null,
) = GradeContext("input", "prompt", output, expected)

class GradersTest {
    @Test
    fun exactMatchTrimsAndComparesCase() =
        runTest {
            val strict = exactMatchGrader()
            assertEquals(Status.Passed, strict.grade(context("  Billing\n", "Billing")).status)
            assertEquals(Status.Failed, strict.grade(context("billing", "Billing")).status)
            assertEquals(Status.Passed, exactMatchGrader(ignoreCase = true).grade(context("billing", "Billing")).status)
        }

    @Test
    fun exactMatchWithoutExpectedIsNotGraded() =
        runTest {
            assertEquals(Status.NotGraded, exactMatchGrader().grade(context("x")).status)
        }

    @Test
    fun regexMustMatchTheWholeOutput() =
        runTest {
            val grader = regexGrader(Regex("[A-Z]{3}-\\d+"))
            assertEquals(Status.Passed, grader.grade(context(" ABC-12 ")).status)
            assertEquals(Status.Failed, grader.grade(context("see ABC-12 now")).status)
        }

    @Test
    fun jsonSchemaAcceptsFencedAndProseWrappedJson() =
        runTest {
            val grader = jsonSchemaGrader(serializer<Ticket>())
            assertEquals(Status.Passed, grader.grade(context("""{"category":"Bug"}""")).status)
            assertEquals(
                Status.Passed,
                grader.grade(context("```json\n{\"category\":\"Bug\",\"urgent\":true}\n```")).status,
            )
            assertEquals(Status.Passed, grader.grade(context("""Sure: {"category":"Bug"} done""")).status)
        }

    @Test
    fun jsonSchemaRejectsMissingFieldsAndNonJson() =
        runTest {
            val grader = jsonSchemaGrader(serializer<Ticket>())
            val missing = grader.grade(context("""{"urgent":true}"""))
            assertEquals(Status.Failed, missing.status)
            assertTrue(missing.rationale.contains("Ticket"))
            assertEquals(Status.Failed, grader.grade(context("no json here")).status)
        }

    @Test
    fun semanticSimilarityComparesAgainstTheThreshold() =
        runTest {
            val embedder = EmbeddingProvider { text -> if (text == "a") listOf(1f, 0f) else listOf(0.9f, 0.1f) }
            val grader = semanticSimilarityGrader(embedder, threshold = 0.9)
            val close = grader.grade(context("b", "a"))
            assertEquals(Status.Passed, close.status)
            assertNotNull(close.score)
            assertEquals(
                Status.Failed,
                semanticSimilarityGrader(embedder, threshold = 0.999).grade(context("b", "a")).status,
            )
        }

    @Test
    fun semanticSimilarityIsNotGradedWithoutUsableEmbeddings() =
        runTest {
            val grader = semanticSimilarityGrader({ text -> if (text == "x") emptyList() else listOf(1f) })
            assertEquals(Status.NotGraded, grader.grade(context("x", "y")).status)
            assertEquals(Status.NotGraded, grader.grade(context("y")).status)
            assertEquals(Status.NotGraded, semanticSimilarityGrader({ listOf(0f, 0f) }).grade(context("a", "b")).status)
        }

    @Test
    fun semanticSimilarityValidatesTheThreshold() {
        assertFailsWithMessage<IllegalArgumentException>("threshold") { semanticSimilarityGrader({ emptyList() }, 1.5) }
    }

    @Test
    fun judgeScoresOnAFourPointScale() =
        runTest {
            fun judge(reply: String) = llmJudgeGrader("rubric", { reply })
            val top = judge("""{"score": 4, "rationale": "good"}""").grade(context("o", "e"))
            assertEquals(Status.Passed, top.status)
            assertEquals(1.0, top.score)
            assertEquals("good", top.rationale)
            assertEquals(Status.Passed, judge("""{"score": 3}""").grade(context("o")).status)
            val low = judge("""```json{"score": 2, "rationale": "vague"}```""").grade(context("o"))
            assertEquals(Status.Failed, low.status)
            assertEquals(1.0 / 3.0, low.score)
        }

    @Test
    fun unreadableJudgeReplyIsNotGraded() =
        runTest {
            for (reply in listOf("great!", """{"score": 7}""", """{"score": "high"}""", "{broken", """[1, 2]""")) {
                assertEquals(Status.NotGraded, llmJudgeGrader("r", { reply }).grade(context("o")).status, reply)
            }
        }

    @Test
    fun judgePromptCarriesRubricInputOutputAndReference() {
        val prompt = judgePrompt("be right", GradeContext("in", "p", "out", "ref"))
        listOf("be right", "in", "out", "Reference answer", "ref").forEach { assertTrue(prompt.contains(it), it) }
        assertTrue(!judgePrompt("r", GradeContext("in", "p", "out", null)).contains("Reference answer"))
    }

    @Test
    fun cosineHandlesMismatchedAndZeroVectors() {
        assertNull(cosine(emptyList(), emptyList()))
        assertNull(cosine(listOf(1f), listOf(1f, 2f)))
        assertNull(cosine(listOf(0f), listOf(1f)))
        assertEquals(1.0, cosine(listOf(2f, 0f), listOf(1f, 0f)))
    }

    @Test
    fun containsGradersCheckSubstrings() =
        runTest {
            assertEquals(Status.Passed, containsGrader("refund").grade(context("Your refund is on its way")).status)
            val missing = containsGrader("refund").grade(context("Hello"))
            assertEquals(Status.Failed, missing.status)
            assertTrue(missing.rationale.contains("refund"))
            assertEquals(Status.Failed, containsGrader("Refund").grade(context("refund")).status)
            assertEquals(Status.Passed, containsGrader("Refund", ignoreCase = true).grade(context("refund")).status)
            assertEquals(Status.Passed, containsAnyGrader("a", "b").grade(context("xb")).status)
            val none = containsAnyGrader("a", "b").grade(context("xyz"))
            assertEquals(Status.Failed, none.status)
            assertTrue(none.rationale.contains("\"a\""))
            assertEquals(Status.Passed, containsAllGrader("order", "refund").grade(context("order refund")).status)
            val partial = containsAllGrader("order", "refund").grade(context("order only"))
            assertEquals(Status.Failed, partial.status)
            assertTrue(partial.rationale.contains("refund") && !partial.rationale.contains("order\""))
            assertEquals(Status.Passed, containsAllGrader("ORDER", ignoreCase = true).grade(context("order")).status)
        }

    @Test
    fun containsAnyAndAllNeedATextToLookFor() {
        assertFailsWithMessage<IllegalArgumentException>("containsAny") { containsAnyGrader() }
        assertFailsWithMessage<IllegalArgumentException>("containsAll") { containsAllGrader() }
    }

    @Test
    fun startsWithIgnoresLeadingWhitespace() =
        runTest {
            assertEquals(Status.Passed, startsWithGrader("Billing").grade(context("  Billing: card")).status)
            val wrong = startsWithGrader("Billing").grade(context("Bug: billing"))
            assertEquals(Status.Failed, wrong.status)
            assertTrue(wrong.rationale.contains("Billing"))
            assertEquals(Status.Passed, startsWithGrader("billing", ignoreCase = true).grade(context("Billing")).status)
        }

    @Test
    fun isJsonAcceptsAnyJsonShapeAfterStrippingFences() =
        runTest {
            val grader = isJsonGrader()
            assertEquals(Status.Passed, grader.grade(context("{\"a\":1}")).status)
            assertEquals(Status.Passed, grader.grade(context("```json\n{\"a\":1}\n```")).status)
            assertEquals(Status.Passed, grader.grade(context("Sure: [1,2] done")).status)
            assertEquals(Status.Failed, grader.grade(context("no json")).status)
            assertEquals(Status.Failed, grader.grade(context("{\"a\":")).status)
            assertEquals(Status.Failed, grader.grade(context("")).status)
        }

    @Test
    fun lengthBetweenIsInclusiveOnTheTrimmedOutput() =
        runTest {
            val grader = lengthBetweenGrader(2, 4)
            assertEquals(Status.Failed, grader.grade(context("a")).status)
            assertEquals(Status.Passed, grader.grade(context("ab")).status)
            assertEquals(Status.Passed, grader.grade(context(" abcd ")).status)
            val long = grader.grade(context("abcde"))
            assertEquals(Status.Failed, long.status)
            assertTrue(long.rationale.contains("5") && long.rationale.contains("2..4"))
        }

    @Test
    fun lengthBetweenRejectsAnInvalidRange() {
        assertFailsWithMessage<IllegalArgumentException>("lengthBetween") { lengthBetweenGrader(5, 2) }
        assertFailsWithMessage<IllegalArgumentException>("lengthBetween") { lengthBetweenGrader(-1, 2) }
    }

    @Test
    fun notInvertsVerdictsAndKeepsNotGraded() =
        runTest {
            val inverted = notGrader(containsGrader("x"))
            assertEquals("not(contains)", inverted.name)
            val failed = inverted.grade(context("has x"))
            assertEquals(Status.Failed, failed.status)
            assertEquals(0.0, failed.score)
            val passed = inverted.grade(context("none"))
            assertEquals(Status.Passed, passed.status)
            assertEquals(1.0, passed.score)
            assertEquals(Status.NotGraded, notGrader(exactMatchGrader()).grade(context("a")).status)
        }
}

internal inline fun <reified T : Throwable> assertFailsWithMessage(
    fragment: String,
    block: () -> Unit,
) {
    val error = kotlin.test.assertFailsWith<T> { block() }
    assertTrue(error.message.orEmpty().contains(fragment), "\"${error.message}\" lacks \"$fragment\"")
}
