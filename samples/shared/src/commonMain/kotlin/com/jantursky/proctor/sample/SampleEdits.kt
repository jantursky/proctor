package com.jantursky.proctor.sample

import com.jantursky.proctor.EvalSuite
import com.jantursky.proctor.evalSuite

/** One case of a sample suite as the dataset editor shows it. */
class SampleCaseDef(
    val suite: String,
    val id: String,
    val input: String,
    /** `null` when the case has no expected value. */
    val expected: String?,
    /** `true` when the user changed the input or the expected value. */
    val edited: Boolean,
)

/**
 * The user's changes to the sample dataset, applied to the suites before a run. Edits live as long as this object
 * does, which is the process, so a restart brings back the defaults.
 */
class SampleEdits {
    private class Edit(
        val input: String,
        val expected: String?,
    )

    private val edits = mutableMapOf<String, Edit>()

    val editedCount: Int get() = edits.size

    /** Every case of every sample suite, with the user's edits applied. */
    fun cases(): List<SampleCaseDef> =
        sampleSuites.flatMap { suite ->
            suite.cases.map { case ->
                val edit = edits[key(suite.name, case.id)]
                SampleCaseDef(
                    suite = suite.name,
                    id = case.id,
                    input = edit?.input ?: case.input,
                    expected = if (edit == null) case.expected else edit.expected,
                    edited = edit != null,
                )
            }
        }

    /** Saves new values for a case. An edit that equals the default is dropped, so the count stays honest. */
    fun set(
        suite: String,
        id: String,
        input: String,
        expected: String?,
    ) {
        val original = sampleSuites.firstOrNull { it.name == suite }?.cases?.firstOrNull { it.id == id } ?: return
        val cleanExpected = expected?.takeIf { it.isNotBlank() }
        if (input == original.input && cleanExpected == original.expected) {
            edits.remove(key(suite, id))
        } else {
            edits[key(suite, id)] = Edit(input, cleanExpected)
        }
    }

    fun reset(
        suite: String,
        id: String,
    ) {
        edits.remove(key(suite, id))
    }

    fun resetAll() {
        edits.clear()
    }

    /** The sample suites with the edits applied: same prompts, graders, repeats and sampling. */
    fun suites(): List<EvalSuite> = sampleSuites.map { source -> withEdits(source) }

    private fun withEdits(source: EvalSuite): EvalSuite =
        evalSuite(source.name) {
            source.prompts.forEach { variant -> prompt(variant.version) { variant.render(it) } }
            source.cases.forEach { case ->
                val edit = edits[key(source.name, case.id)]
                val expected = if (edit == null) case.expected else edit.expected
                case(edit?.input ?: case.input, id = case.id) { if (expected != null) expect(expected) }
            }
            graders { source.graders.forEach { add(it) } }
            repeat(source.repeat)
            sampling(source.sampling)
            passWhen { score >= PASS_SCORES.getValue(source.name) }
        }

    private fun key(
        suite: String,
        id: String,
    ) = "$suite/$id"
}
