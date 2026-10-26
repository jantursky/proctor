package com.jantursky.proctor

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** How a grader, an attempt or a case came out. */
@Serializable
public enum class Status {
    Passed,
    Failed,

    /** Nothing could be judged: quota, backend error, missing expected value. Never a prompt failure. */
    NotGraded,
}

@Serializable
public data class GraderResult(
    val name: String,
    val status: Status,
    val score: Double? = null,
    val rationale: String = "",
)

/** One generation of one case, with every grader's verdict on it. */
@Serializable
public data class Attempt(
    val output: String? = null,
    val latencyMillis: Long = 0,
    val status: Status,
    val score: Double? = null,
    val graders: List<GraderResult> = emptyList(),
    val note: String? = null,
)

/** All attempts of one case; [meanScore] and [spread] (standard deviation) cover the graded attempts only. */
@Serializable
public data class CaseResult(
    val id: String,
    val input: String,
    val expected: String? = null,
    val status: Status,
    val meanScore: Double? = null,
    val spread: Double? = null,
    val attempts: List<Attempt>,
)

@Serializable
public data class Summary(
    val total: Int,
    val passed: Int,
    val failed: Int,
    val notGraded: Int,
    val meanScore: Double? = null,
    /** The suite's `passWhen` verdict; the report exits non-zero when this is `false`. */
    val suitePassed: Boolean,
)

@Serializable
public enum class AvailabilityState { Available, Downloadable, Downloading, Unavailable }

/** The file a runner writes: one prompt version of one suite on one model. */
@Serializable
public data class RunResult(
    val schemaVersion: Int = SCHEMA_VERSION,
    val suite: String,
    val promptVersion: String,
    val environment: Environment,
    val sampling: Sampling,
    val repeat: Int,
    val startedAt: String,
    val availability: AvailabilityState,
    val availabilityReason: String? = null,
    val summary: Summary,
    val cases: List<CaseResult>,
) {
    /** A file name that identifies the run and sorts by time: suite, platform, model, prompt version, start. */
    public fun fileName(): String {
        val parts = listOf(suite, environment.platform, environment.modelVersion ?: "unknown", promptVersion, startedAt)
        return parts.joinToString("_") { it.sanitized() } + ".proctor.json"
    }

    public fun toJson(): String = ProctorJson.encodeToString(serializer(), this)

    public companion object {
        public const val SCHEMA_VERSION: Int = 1

        /** @throws IllegalArgumentException if the text is not a result file or uses a newer schema. */
        public fun fromJson(text: String): RunResult {
            val result =
                try {
                    ProctorJson.decodeFromString(serializer(), text)
                } catch (e: kotlinx.serialization.SerializationException) {
                    throw IllegalArgumentException("Not a Proctor result file: ${e.message}", e)
                }
            require(result.schemaVersion <= SCHEMA_VERSION) {
                "Result file uses schema ${result.schemaVersion}; this version reads up to $SCHEMA_VERSION"
            }
            return result
        }
    }
}

/** The JSON settings of result files. Unknown keys are ignored so older readers open newer files. */
public val ProctorJson: Json =
    Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

private const val FILE_NAME_PART_LIMIT = 40

// Parts are cut so a long model description does not make an unwieldy file name; the JSON holds the full text.
private fun String.sanitized(): String =
    replace(Regex("[^A-Za-z0-9.-]+"), "-")
        .trim('-')
        .take(FILE_NAME_PART_LIMIT)
        .trim('-')
        .ifEmpty { "x" }
