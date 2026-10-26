package com.jantursky.proctor.sample

import com.jantursky.proctor.EvalRunner
import com.jantursky.proctor.RunnerConfig
import com.jantursky.proctor.ios.FoundationModelsBackend
import com.jantursky.proctor.ios.FoundationModelsBridge
import com.jantursky.proctor.ios.ResultStore
import kotlin.time.Duration.Companion.milliseconds

/** What the iOS test target and the simulator script call: run every sample suite and write the result files. */
object SampleRun {
    /**
     * Runs every prompt version of every sample suite on Foundation Models and writes one JSON file per run to
     * [outputDirectory]. Returns the files' paths. Throws if the run cannot continue.
     */
    suspend fun run(
        bridge: FoundationModelsBridge,
        outputDirectory: String,
        requestDelayMillis: Int = 500,
    ): List<String> {
        val runner =
            EvalRunner(FoundationModelsBackend(bridge), RunnerConfig(requestDelay = requestDelayMillis.milliseconds))
        val store = ResultStore(outputDirectory)
        return sampleSuites.flatMap { suite -> runner.runAll(suite).map { store.write(it) } }
    }
}
