package com.jantursky.proctor

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/** One version of the prompt; the suite runs once per version so results compare prompt against prompt. */
public class PromptVariant internal constructor(
    public val version: String,
    private val template: (String) -> String,
) {
    public fun render(input: String): String = template(input)
}

public class EvalCase internal constructor(
    public val id: String,
    public val input: String,
    public val expected: String?,
)

/** What `passWhen` sees: the suite's score so far, as plain numbers. */
public class SuiteScore internal constructor(
    /** Mean of the case scores, over graded cases. `0.0` when nothing was graded. */
    public val score: Double,
    public val total: Int,
    public val passed: Int,
    public val failed: Int,
    public val notGraded: Int,
) {
    /** Share of graded cases that passed, `0.0` when nothing was graded. */
    public val passRate: Double get() = if (passed + failed == 0) 0.0 else passed.toDouble() / (passed + failed)
}

/** A set of cases, graders and settings. Build one with [evalSuite]. */
public class EvalSuite internal constructor(
    public val name: String,
    public val prompts: List<PromptVariant>,
    public val cases: List<EvalCase>,
    public val graders: List<Grader>,
    public val repeat: Int,
    public val sampling: Sampling,
    internal val passWhen: SuiteScore.() -> Boolean,
) {
    public fun prompt(version: String): PromptVariant =
        prompts.firstOrNull { it.version == version }
            ?: throw IllegalArgumentException("No prompt version \"$version\"; declared: ${prompts.map { it.version }}")
}

@DslMarker
public annotation class ProctorDsl

/**
 * Defines a suite:
 *
 * ```
 * val suite = evalSuite("ticket-classifier") {
 *     prompt(version = "v3") { input -> "Classify this support ticket: $input" }
 *     case(input = "My card was charged twice") { expect("Billing") }
 *     graders { exactMatch() }
 *     repeat(5)
 *     passWhen { score >= 0.9 }
 * }
 * ```
 *
 * @throws IllegalArgumentException if the suite has no prompt, no case or no grader.
 */
public fun evalSuite(
    name: String,
    block: EvalSuiteBuilder.() -> Unit,
): EvalSuite = EvalSuiteBuilder(name).apply(block).build()

@ProctorDsl
public class EvalSuiteBuilder internal constructor(
    private val name: String,
) {
    private val prompts = mutableListOf<PromptVariant>()
    private val cases = mutableListOf<EvalCase>()
    private val graders = mutableListOf<Grader>()
    private var repeat = 1
    private var sampling = Sampling()
    private var passWhen: SuiteScore.() -> Boolean = { failed == 0 && notGraded < total }

    /** Adds a prompt version. Declare several to compare prompt versions in one run. */
    public fun prompt(
        version: String,
        template: (input: String) -> String,
    ) {
        require(prompts.none { it.version == version }) { "Prompt version \"$version\" is declared twice" }
        prompts += PromptVariant(version, template)
    }

    public fun case(
        input: String,
        id: String? = null,
        block: CaseBuilder.() -> Unit = {},
    ) {
        val caseId = id ?: "case-${cases.size + 1}"
        require(cases.none { it.id == caseId }) { "Case id \"$caseId\" is declared twice" }
        cases += EvalCase(caseId, input, CaseBuilder().apply(block).expected)
    }

    public fun graders(block: GradersBuilder.() -> Unit) {
        graders += GradersBuilder().apply(block).graders
    }

    /** How many times each case runs. Scores are reported as mean and spread. */
    public fun repeat(times: Int) {
        require(times >= 1) { "repeat must be at least 1" }
        repeat = times
    }

    public fun sampling(sampling: Sampling) {
        this.sampling = sampling
    }

    /** The suite verdict. The default passes when no case failed and at least one was graded. */
    public fun passWhen(predicate: SuiteScore.() -> Boolean) {
        passWhen = predicate
    }

    internal fun build(): EvalSuite {
        require(prompts.isNotEmpty()) { "Suite \"$name\" needs a prompt" }
        require(cases.isNotEmpty()) { "Suite \"$name\" needs at least one case" }
        require(graders.isNotEmpty()) { "Suite \"$name\" needs at least one grader" }
        return EvalSuite(name, prompts.toList(), cases.toList(), graders.toList(), repeat, sampling, passWhen)
    }
}

@ProctorDsl
public class CaseBuilder internal constructor() {
    internal var expected: String? = null

    /** The reference answer. Anything printable works; it is stored as its `toString()`. */
    public fun expect(value: Any) {
        expected = value.toString()
    }
}

@ProctorDsl
public class GradersBuilder internal constructor() {
    internal val graders = mutableListOf<Grader>()

    public fun add(grader: Grader) {
        graders += grader
    }

    public fun exactMatch(ignoreCase: Boolean = false) {
        add(exactMatchGrader(ignoreCase))
    }

    public fun regex(pattern: Regex) {
        add(regexGrader(pattern))
    }

    public inline fun <reified T> jsonSchema(json: Json = Json.Default) {
        add(jsonSchemaGrader(serializer<T>(), json))
    }

    public fun semanticSimilarity(
        embedder: EmbeddingProvider,
        threshold: Double = 0.8,
    ) {
        add(semanticSimilarityGrader(embedder, threshold))
    }

    public fun llmJudge(
        rubric: String,
        judge: Judge,
    ) {
        add(llmJudgeGrader(rubric, judge))
    }
}
