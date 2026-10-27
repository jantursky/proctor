package com.jantursky.proctor.sample

import com.jantursky.proctor.BackendException
import com.jantursky.proctor.ModelBackend
import com.jantursky.proctor.RunResult
import com.jantursky.proctor.Sampling
import com.jantursky.proctor.Status

/** One attempt of a case: what the model said and how it was judged. */
class SampleAttempt(
    val output: String?,
    val status: Status,
    /** Why a grader failed it, or why it could not be graded. */
    val reason: String?,
)

/** One case of a run, with the attempts behind its verdict. */
class SampleCase(
    val id: String,
    val input: String,
    val expected: String?,
    val status: Status,
    val meanScore: Double?,
    val attempts: List<SampleAttempt>,
)

/** One finished run, as the sample apps list it. */
class SampleRow(
    val suite: String,
    val promptVersion: String,
    val passed: Int,
    val failed: Int,
    val notGraded: Int,
    val total: Int,
    /** `null` when nothing was graded. */
    val meanScore: Double?,
    val suitePassed: Boolean,
    /** When the run started, as written in the result file (UTC, ISO 8601). */
    val startedAt: String,
    val deviceModel: String,
    val osVersion: String,
    val modelVersion: String?,
    val backend: String,
    /** Why the model was not available, `null` when it was. */
    val availabilityNote: String?,
    /** Runs started together share it; `null` for files written before runs were grouped. */
    val groupId: String?,
    val cases: List<SampleCase>,
)

fun RunResult.toSampleRow(): SampleRow =
    SampleRow(
        suite = suite,
        promptVersion = promptVersion,
        passed = summary.passed,
        failed = summary.failed,
        notGraded = summary.notGraded,
        total = summary.total,
        meanScore = summary.meanScore,
        suitePassed = summary.suitePassed,
        startedAt = startedAt,
        deviceModel = environment.deviceModel,
        osVersion = environment.osVersion,
        modelVersion = environment.modelVersion,
        backend = environment.backend,
        availabilityNote = availabilityReason,
        groupId = groupId,
        cases =
            cases.map { case ->
                SampleCase(
                    id = case.id,
                    input = case.input,
                    expected = case.expected,
                    status = case.status,
                    meanScore = case.meanScore,
                    attempts =
                        case.attempts.map { attempt ->
                            SampleAttempt(
                                output = attempt.output,
                                status = attempt.status,
                                reason =
                                    attempt.note
                                        ?: attempt.graders
                                            .firstOrNull { it.status != Status.Passed }
                                            ?.rationale
                                            ?.ifBlank { null },
                            )
                        },
                )
            },
    )

/** Runs that were started together, as the history screen lists them. */
class SampleGroup(
    /** The shared group id, `null` for the bucket of files written before runs were grouped. */
    val id: String?,
    /** When the first run of the group started. */
    val startedAt: String,
    /** The runs, earliest first. */
    val rows: List<SampleRow>,
) {
    val backend: String get() = rows.first().backend
    val modelVersion: String? get() = rows.first().modelVersion
    val allPassed: Boolean get() = rows.all { it.suitePassed }
}

/**
 * Groups [rows] by the group id their runs share, newest group first. Runs without an id go into one group of their
 * own, so old files stay visible without pretending they ran together.
 */
fun groupHistory(rows: List<SampleRow>): List<SampleGroup> =
    rows
        .groupBy { it.groupId }
        .map { (id, members) ->
            val ordered = members.sortedBy { it.startedAt }
            SampleGroup(id, ordered.first().startedAt, ordered)
        }.sortedByDescending { it.startedAt }

/**
 * Reads a result file written by an earlier run, for the history screen. Returns `null` for a file that is not a
 * readable result of this schema version, so one broken file does not hide the others.
 */
fun parseSampleRow(json: String): SampleRow? =
    try {
        RunResult.fromJson(json).toSampleRow()
    } catch (e: IllegalArgumentException) {
        null
    }

/** Model requests a full run of [sampleSuites] sends, retries not counted. */
val sampleRequestCount: Int = sampleSuites.sumOf { it.prompts.size * it.cases.size * it.repeat }

/**
 * Counts finished requests so a screen can show a progress bar. A request that AICore pushes back with a quota error
 * is retried by the runner and counted once, when it finishes.
 */
class ProgressBackend(
    private val delegate: ModelBackend,
    private val total: Int,
    private val onProgress: (done: Int, total: Int) -> Unit,
    private val onWaiting: () -> Unit,
) : ModelBackend by delegate {
    private var done = 0

    override suspend fun generate(
        prompt: String,
        sampling: Sampling,
    ): String {
        val reply =
            try {
                delegate.generate(prompt, sampling)
            } catch (e: BackendException.QuotaExceeded) {
                onWaiting()
                throw e
            } catch (e: BackendException) {
                count()
                throw e
            }
        count()
        return reply
    }

    private fun count() {
        done++
        onProgress(done.coerceAtMost(total), total)
    }
}
