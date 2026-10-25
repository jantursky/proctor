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
}

internal inline fun <reified T : Throwable> assertFailsWithMessage(
    fragment: String,
    block: () -> Unit,
) {
    val error = kotlin.test.assertFailsWith<T> { block() }
    assertTrue(error.message.orEmpty().contains(fragment), "\"${error.message}\" lacks \"$fragment\"")
}
