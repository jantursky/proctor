package com.jantursky.proctor.report

import com.jantursky.proctor.Attempt
import com.jantursky.proctor.AvailabilityState
import com.jantursky.proctor.CaseResult
import com.jantursky.proctor.Environment
import com.jantursky.proctor.GraderResult
import com.jantursky.proctor.RunResult
import com.jantursky.proctor.Sampling
import com.jantursky.proctor.Status
import com.jantursky.proctor.Summary

internal fun case(
    id: String,
    status: Status,
    output: String? = "out $id",
    score: Double? =
        if (status == Status.Passed) {
            1.0
        } else if (status == Status.Failed) {
            0.0
        } else {
            null
        },
    input: String = "input $id",
) = CaseResult(
    id = id,
    input = input,
    expected = "A",
    status = status,
    meanScore = score,
    spread = score?.let { 0.0 },
    attempts =
        listOf(
            Attempt(
                output = output,
                status = status,
                score = score,
                graders = listOf(GraderResult("exactMatch", status, score, "because")),
                note = if (status == Status.NotGraded) "quota" else null,
            ),
        ),
)

internal fun run(
    vararg cases: CaseResult,
    suite: String = "tickets",
    platform: String = "android",
    modelVersion: String? = "nano-v3",
    prompt: String = "v1",
    startedAt: String = "2026-10-26T10:00:00Z",
    availability: AvailabilityState = AvailabilityState.Available,
    suitePassed: Boolean = cases.none { it.status == Status.Failed },
): RunResult {
    val graded = cases.mapNotNull { it.meanScore }
    return RunResult(
        suite = suite,
        promptVersion = prompt,
        environment = Environment(platform, "Galaxy S26", "16", modelVersion, "mlkit"),
        sampling = Sampling(),
        repeat = 1,
        startedAt = startedAt,
        availability = availability,
        availabilityReason = if (availability == AvailabilityState.Unavailable) "bootloader" else null,
        summary =
            Summary(
                total = cases.size,
                passed = cases.count { it.status == Status.Passed },
                failed = cases.count { it.status == Status.Failed },
                notGraded = cases.count { it.status == Status.NotGraded },
                meanScore = if (graded.isEmpty()) null else graded.sum() / graded.size,
                suitePassed = suitePassed,
            ),
        cases = cases.toList(),
    )
}
