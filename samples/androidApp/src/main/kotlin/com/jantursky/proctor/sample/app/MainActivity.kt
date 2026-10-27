package com.jantursky.proctor.sample.app

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.jantursky.proctor.BackendAvailability
import com.jantursky.proctor.EvalRunner
import com.jantursky.proctor.RunAbortedException
import com.jantursky.proctor.android.ForegroundBackend
import com.jantursky.proctor.android.MlKitBackend
import com.jantursky.proctor.android.MlKitConfig
import com.jantursky.proctor.android.ModelDownload
import com.jantursky.proctor.android.NanoPreference
import com.jantursky.proctor.android.NanoRelease
import com.jantursky.proctor.android.ResultStore
import com.jantursky.proctor.sample.sampleSuites
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Runs the sample suites on Gemini Nano while this activity is the foreground app, which is what AICore requires.
 * Start it with the launcher icon: an instrumented test restarts the app process, so it cannot run beside this
 * screen. Result files land in the app's external files directory.
 */
class MainActivity : Activity() {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private var config = MlKitConfig()

    @Volatile
    private var resumed = false
    private lateinit var output: TextView
    private lateinit var status: TextView
    private lateinit var releaseButton: Button
    private lateinit var preferenceButton: Button
    private lateinit var downloadButton: Button
    private lateinit var runButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        output = TextView(this)
        status = TextView(this)
        releaseButton = Button(this).apply { setOnClickListener { toggleRelease() } }
        preferenceButton = Button(this).apply { setOnClickListener { togglePreference() } }
        downloadButton = Button(this).apply { text = "Download model" }
        downloadButton.setOnClickListener { download() }
        runButton = Button(this).apply { text = "Run sample suites" }
        runButton.setOnClickListener { run() }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                // Edge-to-edge is enforced on recent Android versions; this keeps the content below the status bar.
                fitsSystemWindows = true
                setPadding(48, 48, 48, 48)
                addView(releaseButton)
                addView(preferenceButton)
                addView(status)
                addView(downloadButton)
                addView(runButton)
                addView(ScrollView(context).apply { addView(output) })
            },
        )
        refresh()
    }

    private fun toggleRelease() {
        config =
            config.copy(release = if (config.release == NanoRelease.Stable) NanoRelease.Preview else NanoRelease.Stable)
        refresh()
    }

    private fun togglePreference() {
        config =
            config.copy(
                preference =
                    if (config.preference ==
                        NanoPreference.Full
                    ) {
                        NanoPreference.Fast
                    } else {
                        NanoPreference.Full
                    },
            )
        refresh()
    }

    /** Shows the model state, so a device without the Prompt API says so instead of failing a run. */
    private fun refresh() {
        releaseButton.text = "Release: ${config.release.name}"
        preferenceButton.text = "Variant: ${config.preference.name}"
        downloadButton.isEnabled = false
        downloadButton.visibility = android.view.View.GONE
        runButton.isEnabled = false
        status.text = "Checking Gemini Nano..."
        scope.launch {
            val availability =
                try {
                    MlKitBackend(config).availability()
                } catch (e: Exception) {
                    BackendAvailability.Unavailable("could not be checked (${e::class.simpleName}: ${e.message})")
                }
            runOnUiThread { show(availability) }
        }
    }

    private fun show(availability: BackendAvailability) {
        status.text =
            when (availability) {
                BackendAvailability.Available -> "Gemini Nano: available"
                BackendAvailability.Downloadable -> "Gemini Nano: supported, not downloaded yet"
                BackendAvailability.Downloading -> "Gemini Nano: downloading, check again in a moment"
                is BackendAvailability.Unavailable -> "Gemini Nano: unavailable. ${availability.reason}"
            }
        runButton.isEnabled = availability == BackendAvailability.Available
        downloadButton.isEnabled = true
        downloadButton.visibility =
            if (availability == BackendAvailability.Downloadable) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun download() {
        downloadButton.isEnabled = false
        scope.launch {
            MlKitBackend(config).download().collect { step ->
                runOnUiThread {
                    status.text =
                        when (step) {
                            is ModelDownload.Started -> "Downloading ${step.bytesToDownload / MEGABYTE} MB..."
                            is ModelDownload.Progress -> "Downloaded ${step.bytesDownloaded / MEGABYTE} MB"
                            ModelDownload.Completed -> "Download finished, checking the model..."
                            is ModelDownload.Failed -> "Download failed: ${step.reason}"
                        }
                }
                if (step == ModelDownload.Completed) runOnUiThread { refresh() }
                if (step is ModelDownload.Failed) runOnUiThread { downloadButton.isEnabled = true }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
    }

    override fun onPause() {
        resumed = false
        super.onPause()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun run() {
        if (job?.isActive == true) return
        runButton.isEnabled = false
        output.text = ""
        val store = ResultStore.forApp(this)
        job =
            scope.launch {
                val backend = ForegroundBackend(MlKitBackend(config), ::resumedNow)
                try {
                    for (suite in sampleSuites) {
                        for (result in EvalRunner(backend).runAll(suite)) {
                            store.write(result)
                            val s = result.summary
                            append(
                                "${suite.name} ${result.promptVersion}: ${s.passed}/${s.total} passed, ${s.notGraded} not graded",
                            )
                        }
                    }
                    append("Done. Files in ${store.directory}")
                } catch (e: RunAbortedException) {
                    append("Run aborted: ${e.message}")
                } finally {
                    runOnUiThread { runButton.isEnabled = true }
                }
            }
    }

    private fun resumedNow() = resumed

    private fun append(line: String) = runOnUiThread { output.append(line + "\n") }
}

private const val MEGABYTE = 1_000_000L
