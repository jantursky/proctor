package com.jantursky.proctor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val passedGrader = GraderResult("exactMatch", Status.Passed, 1.0)

private fun sample(
    modelVersion: String? = "nano-v3",
    suite: String = "ticket classifier",
) = RunResult(
    suite = suite,
    promptVersion = "v3",
    environment = Environment("android", "Galaxy S26", "16", modelVersion, "mlkit"),
    sampling = Sampling(temperature = 0.1),
    repeat = 2,
    startedAt = "2026-10-26T10:00:00Z",
    availability = AvailabilityState.Available,
    summary = Summary(1, 1, 0, 0, 1.0, suitePassed = true),
    cases =
        listOf(
            CaseResult(
                id = "case-1",
                input = "hi",
                expected = "A",
                status = Status.Passed,
                meanScore = 1.0,
                spread = 0.0,
                attempts = listOf(Attempt("A", 12, Status.Passed, 1.0, listOf(passedGrader))),
            ),
        ),
)

class ResultTest {
    @Test
    fun roundTripsThroughJson() {
        val result = sample()
        assertEquals(result, RunResult.fromJson(result.toJson()))
    }

    @Test
    fun readsFilesWithUnknownFields() {
        val json = sample().toJson().replaceFirst("{", """{"futureField": 1,""")
        assertEquals(sample(), RunResult.fromJson(json))
    }

    @Test
    fun rejectsNewerSchemasAndNonResultFiles() {
        val newer = sample().toJson().replace("\"schemaVersion\": 1", "\"schemaVersion\": 99")
        assertFailsWithMessage<IllegalArgumentException>("schema 99") { RunResult.fromJson(newer) }
        assertFailsWithMessage<IllegalArgumentException>("Not a Proctor") { RunResult.fromJson("{}") }
    }

    @Test
    fun fileNameIdentifiesTheRun() {
        assertEquals(
            "ticket-classifier_android_nano-v3_v3_2026-10-26T10-00-00Z.proctor.json",
            sample().fileName(),
        )
        assertTrue(sample(modelVersion = null).fileName().contains("_unknown_"))
        assertTrue(sample(suite = "///").fileName().startsWith("x_"))
        val long = sample(modelVersion = "m".repeat(100)).fileName().split("_")
        assertEquals(40, long[2].length)
    }

    @Test
    fun statsAreMeanAndPopulationSpread() {
        assertEquals(null, emptyList<Double>().meanOrNull())
        assertEquals(null, emptyList<Double>().spreadOrNull())
        assertEquals(2.0, listOf(1.0, 2.0, 3.0).meanOrNull())
        assertEquals(1.0, listOf(1.0, 3.0).spreadOrNull())
    }
}
