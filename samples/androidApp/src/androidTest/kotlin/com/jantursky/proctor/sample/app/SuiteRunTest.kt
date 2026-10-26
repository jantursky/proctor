package com.jantursky.proctor.sample.app

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.jantursky.proctor.EvalRunner
import com.jantursky.proctor.android.ForegroundBackend
import com.jantursky.proctor.android.MlKitBackend
import com.jantursky.proctor.android.MlKitConfig
import com.jantursky.proctor.android.NanoPreference
import com.jantursky.proctor.android.NanoRelease
import com.jantursky.proctor.android.ProctorHostActivity
import com.jantursky.proctor.android.ResultStore
import com.jantursky.proctor.sample.sampleSuites
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs every sample suite on Gemini Nano through the ML Kit Prompt API and writes one JSON file per run. Needs a
 * physical device with a locked bootloader and the model downloaded; an emulator has no AICore.
 *
 * Pick the model with instrumentation arguments (defaults: the stable model):
 * `-e proctor.release preview -e proctor.preference fast`
 */
class SuiteRunTest {
    @Test
    fun runSampleSuites() {
        val arguments = InstrumentationRegistry.getArguments()
        val config =
            MlKitConfig(
                release = arguments.getString("proctor.release").toEnum(NanoRelease.Stable),
                preference = arguments.getString("proctor.preference").toEnum(NanoPreference.Full),
            )
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = ResultStore.forApp(context)

        ActivityScenario.launch(ProctorHostActivity::class.java).use {
            val backend = ForegroundBackend(MlKitBackend(config), ProctorHostActivity::isForeground)
            val results = runBlocking { sampleSuites.flatMap { EvalRunner(backend).runAll(it) } }
            results.forEach { store.write(it) }
            // A model that is missing is reported in the files; the test only fails when nothing could be read.
            assertTrue("No results written", results.isNotEmpty())
            println("Proctor wrote ${results.size} result files to ${store.directory}")
        }
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    enumValues<T>().firstOrNull { it.name.equals(this, ignoreCase = true) } ?: default
