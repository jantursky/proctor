package com.jantursky.proctor

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Serializable
private data class Shape(
    val name: String,
)

class SuiteTest {
    private fun suite(block: EvalSuiteBuilder.() -> Unit = {}) =
        evalSuite("s") {
            prompt("v1") { "Classify: $it" }
            case("a") { expect("A") }
            graders { exactMatch() }
            block()
        }

    @Test
    fun buildsCasesWithDefaultIdsAndExpectedValues() {
        val suite =
            suite {
                case("b", id = "named")
                case("c") { expect(42) }
            }
        assertEquals(listOf("case-1", "named", "case-3"), suite.cases.map { it.id })
        assertEquals(listOf("A", null, "42"), suite.cases.map { it.expected })
        assertEquals("Classify: a", suite.prompt("v1").render("a"))
    }

    @Test
    fun registersEveryGraderKind() {
        val suite =
            suite {
                graders {
                    regex(Regex(".+"))
                    jsonSchema<Shape>()
                    semanticSimilarity({ listOf(1f) })
                    llmJudge("r", { "" })
                    add(exactMatchGrader())
                }
            }
        assertEquals(
            listOf("exactMatch", "regex", "jsonSchema", "semanticSimilarity", "llmJudge", "exactMatch"),
            suite.graders.map { it.name },
        )
    }

    @Test
    fun keepsRepeatSamplingAndPromptVersions() {
        val sampling = Sampling(temperature = 0.2, topK = 16)
        val suite =
            suite {
                repeat(5)
                sampling(sampling)
                prompt("v2") { it }
            }
        assertEquals(5, suite.repeat)
        assertEquals(sampling, suite.sampling)
        assertEquals(listOf("v1", "v2"), suite.prompts.map { it.version })
    }

    @Test
    fun defaultVerdictNeedsNoFailureAndOneGradedCase() {
        val verdict = suite().passWhen
        assertTrue(SuiteScore(1.0, 2, 2, 0, 0).verdict(verdict))
        assertTrue(!SuiteScore(0.5, 2, 1, 1, 0).verdict(verdict))
        assertTrue(!SuiteScore(0.0, 2, 0, 0, 2).verdict(verdict))
    }

    @Test
    fun passRateCountsGradedCasesOnly() {
        assertEquals(0.5, SuiteScore(0.5, 4, 1, 1, 2).passRate)
        assertEquals(0.0, SuiteScore(0.0, 2, 0, 0, 2).passRate)
    }

    @Test
    fun rejectsIncompleteOrInconsistentSuites() {
        assertFailsWithMessage<IllegalArgumentException>("needs a prompt") {
            evalSuite("s") {
                case("a")
                graders { exactMatch() }
            }
        }
        assertFailsWithMessage<IllegalArgumentException>("at least one case") {
            evalSuite("s") {
                prompt("v") { it }
                graders { exactMatch() }
            }
        }
        assertFailsWithMessage<IllegalArgumentException>("at least one grader") {
            evalSuite("s") {
                prompt("v") { it }
                case("a")
            }
        }
        assertFailsWithMessage<IllegalArgumentException>("declared twice") { suite { prompt("v1") { it } } }
        assertFailsWithMessage<IllegalArgumentException>("declared twice") { suite { case("x", id = "case-1") } }
        assertFailsWithMessage<IllegalArgumentException>("repeat") { suite { repeat(0) } }
        assertFailsWithMessage<IllegalArgumentException>("No prompt version") { suite().prompt("nope") }
    }

    private fun SuiteScore.verdict(predicate: SuiteScore.() -> Boolean) = predicate()
}
