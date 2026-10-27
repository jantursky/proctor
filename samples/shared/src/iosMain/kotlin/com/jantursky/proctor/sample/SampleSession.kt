package com.jantursky.proctor.sample

import com.jantursky.proctor.EvalRunner
import com.jantursky.proctor.ModelBackend
import com.jantursky.proctor.RunAbortedException
import com.jantursky.proctor.RunnerConfig
import com.jantursky.proctor.Sampling
import com.jantursky.proctor.ios.FoundationModelsBackend
import com.jantursky.proctor.ios.FoundationModelsBridge
import com.jantursky.proctor.ios.ResultStore
import kotlin.time.Duration.Companion.milliseconds

/** One finished run, as the sample app lists it. */
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
)

/** What the SwiftUI sample app implements to follow a run. */
interface SampleListener {
    fun onProgress(text: String)

    fun onResult(row: SampleRow)
}

/** Runs the sample suites for the iOS sample app and reports each finished run to [listener]. */
object SampleSession {
    /**
     * Returns the number of result files written, or throws [RunAbortedException] when the run cannot continue.
     * Files go to [outputDirectory], one JSON file per suite and prompt version.
     */
    suspend fun run(
        bridge: FoundationModelsBridge,
        outputDirectory: String,
        listener: SampleListener,
    ): Int {
        val backend = Observing(FoundationModelsBackend(bridge), listener)
        val runner = EvalRunner(backend, RunnerConfig(requestDelay = 500.milliseconds))
        val store = ResultStore(outputDirectory)
        var written = 0
        for (suite in sampleSuites) {
            for (result in runner.runAll(suite)) {
                store.write(result)
                written++
                val s = result.summary
                listener.onResult(
                    SampleRow(suite.name, result.promptVersion, s.passed, s.failed, s.notGraded, s.total, s.meanScore, s.suitePassed),
                )
            }
        }
        return written
    }
}

private class Observing(
    private val delegate: ModelBackend,
    private val listener: SampleListener,
) : ModelBackend by delegate {
    private var requests = 0

    override suspend fun generate(
        prompt: String,
        sampling: Sampling,
    ): String {
        listener.onProgress("Request ${++requests} sent to Foundation Models")
        return delegate.generate(prompt, sampling)
    }
}
