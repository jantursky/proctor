package com.jantursky.proctor.sample

import com.jantursky.proctor.EvalRunner
import com.jantursky.proctor.RunAbortedException
import com.jantursky.proctor.RunnerConfig
import com.jantursky.proctor.ios.FoundationModelsBackend
import com.jantursky.proctor.ios.FoundationModelsBridge
import com.jantursky.proctor.ios.ResultStore
import kotlin.time.Duration.Companion.milliseconds

/** What the SwiftUI sample app implements to follow a run. */
interface SampleListener {
    /** [done] of [total] requests have finished. */
    fun onProgress(
        done: Int,
        total: Int,
    )

    /** The system model asked the runner to slow down; it waits and retries. */
    fun onWaiting()

    fun onResult(row: SampleRow)
}

/** Runs the sample suites for the iOS sample app and reports progress and each finished run to [listener]. */
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
        val total = sampleRequestCount
        listener.onProgress(0, total)
        val backend = ProgressBackend(FoundationModelsBackend(bridge), total, listener::onProgress, listener::onWaiting)
        val runner = EvalRunner(backend, RunnerConfig(requestDelay = 500.milliseconds))
        val store = ResultStore(outputDirectory)
        var written = 0
        for (suite in sampleSuites) {
            // One prompt version at a time, so each row shows up as soon as its run finishes.
            for (prompt in suite.prompts) {
                val result = runner.run(suite, prompt.version)
                store.write(result)
                written++
                listener.onResult(result.toSampleRow())
            }
        }
        return written
    }
}
