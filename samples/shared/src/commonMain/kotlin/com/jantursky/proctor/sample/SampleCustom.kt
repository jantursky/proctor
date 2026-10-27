package com.jantursky.proctor.sample

import com.jantursky.proctor.EvalRunner
import com.jantursky.proctor.EvalSuite
import com.jantursky.proctor.ModelBackend
import com.jantursky.proctor.RunnerConfig
import com.jantursky.proctor.evalSuite
import kotlin.time.Duration.Companion.milliseconds

/**
 * A copy of the sample suite [suiteName] with one case of the user's own: same prompt versions, graders, repeats and
 * sampling, so the verdict means the same as in the full run. `null` for an unknown suite.
 */
fun customCaseSuite(
    suiteName: String,
    input: String,
    expected: String?,
): EvalSuite? {
    val source = sampleSuites.firstOrNull { it.name == suiteName } ?: return null
    return evalSuite(source.name) {
        source.prompts.forEach { variant -> prompt(variant.version) { variant.render(it) } }
        case(input, id = CUSTOM_CASE_ID) { if (!expected.isNullOrBlank()) expect(expected) }
        graders { source.graders.forEach { add(it) } }
        repeat(source.repeat)
        sampling(source.sampling)
    }
}

/**
 * Runs the user's own case against one prompt version and returns how it came out, or `null` for an unknown suite.
 * Nothing is saved: a custom case is a quick check, not a record.
 */
suspend fun runCustomCase(
    backend: ModelBackend,
    suiteName: String,
    promptVersion: String,
    input: String,
    expected: String?,
): SampleCase? {
    val suite = customCaseSuite(suiteName, input, expected) ?: return null
    val runner = EvalRunner(backend, RunnerConfig(requestDelay = 500.milliseconds))
    return runner
        .run(suite, promptVersion)
        .toSampleRow()
        .cases
        .single()
}

private const val CUSTOM_CASE_ID = "custom"
