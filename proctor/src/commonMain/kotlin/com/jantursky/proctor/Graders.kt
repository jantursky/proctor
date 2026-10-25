package com.jantursky.proctor

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.sqrt

/** Passes when the trimmed output equals the expected value. */
public fun exactMatchGrader(ignoreCase: Boolean = false): Grader =
    object : Grader {
        override val name = "exactMatch"

        override suspend fun grade(context: GradeContext): Grade {
            val expected = context.expected ?: return Grade.notGraded("The case has no expected value")
            val output = context.output.trim()
            return if (output.equals(expected.trim(), ignoreCase)) {
                Grade.passed()
            } else {
                Grade.failed(rationale = "Expected \"${expected.trim()}\", got \"$output\"")
            }
        }
    }

/** Passes when [pattern] matches the whole trimmed output. */
public fun regexGrader(pattern: Regex): Grader =
    object : Grader {
        override val name = "regex"

        override suspend fun grade(context: GradeContext): Grade =
            if (pattern.matches(context.output.trim())) {
                Grade.passed()
            } else {
                Grade.failed(rationale = "Output does not match ${pattern.pattern}")
            }
    }

/** Passes when the output, after removing Markdown code fences, decodes into [serializer]'s type. */
public fun <T> jsonSchemaGrader(
    serializer: KSerializer<T>,
    json: Json = Json.Default,
): Grader =
    object : Grader {
        override val name = "jsonSchema"

        override suspend fun grade(context: GradeContext): Grade =
            try {
                json.decodeFromString(serializer, extractJson(context.output))
                Grade.passed()
            } catch (e: IllegalArgumentException) {
                // SerializationException is an IllegalArgumentException, and so are malformed-input errors.
                Grade.failed(
                    rationale = "Not valid ${serializer.descriptor.serialName}: ${e.message?.lineSequence()?.first()}",
                )
            }
    }

/** Turns text into a vector. Implement it with whatever embedding model you run, on or off the device. */
public fun interface EmbeddingProvider {
    public suspend fun embed(text: String): List<Float>
}

/** Scores the cosine similarity of the output and the expected value; passes at or above [threshold]. */
public fun semanticSimilarityGrader(
    embedder: EmbeddingProvider,
    threshold: Double = 0.8,
): Grader {
    require(threshold in 0.0..1.0) { "threshold must be between 0 and 1" }
    return object : Grader {
        override val name = "semanticSimilarity"

        override suspend fun grade(context: GradeContext): Grade {
            val expected = context.expected ?: return Grade.notGraded("The case has no expected value")
            val similarity =
                cosine(embedder.embed(context.output), embedder.embed(expected))
                    ?: return Grade.notGraded("Embeddings are empty or differ in size")
            val score = similarity.coerceIn(0.0, 1.0)
            val rationale = "Similarity ${formatTwo(similarity)}, threshold ${formatTwo(threshold)}"
            return if (similarity >= threshold) Grade.passed(score, rationale) else Grade.failed(score, rationale)
        }
    }
}

/** A model that judges outputs, typically a cloud model run on the host, not on the device. */
public fun interface Judge {
    public suspend fun judge(prompt: String): String
}

/**
 * Asks [judge] to score the output against [rubric] on a 1 to 4 scale (an even scale has no neutral middle) and to
 * explain the score in one sentence. 3 and 4 pass. Judge output that cannot be read is [Status.NotGraded].
 */
public fun llmJudgeGrader(
    rubric: String,
    judge: Judge,
): Grader =
    object : Grader {
        override val name = "llmJudge"

        override suspend fun grade(context: GradeContext): Grade {
            val reply = judge.judge(judgePrompt(rubric, context))
            val verdict =
                parseVerdict(reply) ?: return Grade.notGraded("Could not read the judge's reply: ${reply.take(120)}")
            val score = (verdict.first - 1) / 3.0
            return if (verdict.first >= 3) Grade.passed(score, verdict.second) else Grade.failed(score, verdict.second)
        }
    }

internal fun judgePrompt(
    rubric: String,
    context: GradeContext,
): String =
    buildString {
        appendLine("You grade the output of a language model against a rubric.")
        appendLine("Rubric: $rubric")
        appendLine("Input given to the model:")
        appendLine(context.input)
        appendLine("Model output:")
        appendLine(context.output)
        context.expected?.let {
            appendLine("Reference answer:")
            appendLine(it)
        }
        append("Score from 1 (fails the rubric) to 4 (fully meets it). ")
        append("Reply with JSON only: {\"score\": <1-4>, \"rationale\": \"<one sentence>\"}")
    }

internal fun parseVerdict(reply: String): Pair<Int, String>? {
    val parsed =
        try {
            Json.parseToJsonElement(extractJson(reply)) as? JsonObject
        } catch (e: SerializationException) {
            null
        } ?: return null
    val score = (parsed["score"] as? JsonPrimitive)?.intOrNull?.takeIf { it in 1..4 } ?: return null
    val rationale = parsed["rationale"]?.jsonPrimitive?.content.orEmpty()
    return score to rationale
}

/** Strips Markdown fences and surrounding prose so `Here you go: ```json {...}``` ` still parses. */
internal fun extractJson(text: String): String {
    val trimmed =
        text
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    val start = trimmed.indexOfFirst { it == '{' || it == '[' }
    val end = trimmed.indexOfLast { it == '}' || it == ']' }
    return if (start in 0..<end) trimmed.substring(start, end + 1) else trimmed
}

internal fun cosine(
    a: List<Float>,
    b: List<Float>,
): Double? {
    if (a.isEmpty() || a.size != b.size) return null
    var dot = 0.0
    var normA = 0.0
    var normB = 0.0
    for (i in a.indices) {
        dot += a[i] * b[i]
        normA += a[i] * a[i]
        normB += b[i] * b[i]
    }
    if (normA == 0.0 || normB == 0.0) return null
    return dot / (sqrt(normA) * sqrt(normB))
}

private fun formatTwo(value: Double): String = (kotlin.math.round(value * 100) / 100).toString()
