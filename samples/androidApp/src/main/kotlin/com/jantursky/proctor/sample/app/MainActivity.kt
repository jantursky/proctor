package com.jantursky.proctor.sample.app

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.jantursky.proctor.BackendAvailability
import com.jantursky.proctor.BackendException
import com.jantursky.proctor.EvalRunner
import com.jantursky.proctor.ModelBackend
import com.jantursky.proctor.RunAbortedException
import com.jantursky.proctor.Sampling
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Runs the sample suites on Gemini Nano while this activity is the foreground app, which is what AICore requires.
 * Start it with the launcher icon: an instrumented test restarts the app process, so it cannot run beside this
 * screen. Result files land in the app's external files directory.
 */
class MainActivity : Activity() {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private var downloadJob: Job? = null
    private var pollJob: Job? = null
    private var config = MlKitConfig()

    @Volatile
    private var resumed = false
    private lateinit var output: TextView
    private lateinit var status: TextView
    private lateinit var releaseButton: Button
    private lateinit var preferenceButton: Button
    private lateinit var downloadButton: Button
    private lateinit var checkButton: Button
    private lateinit var runButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        output = TextView(this)
        status = TextView(this)
        releaseButton = Button(this).apply { setOnClickListener { toggleRelease() } }
        preferenceButton = Button(this).apply { setOnClickListener { togglePreference() } }
        downloadButton = Button(this).apply { text = "Download model" }
        downloadButton.setOnClickListener { if (downloadJob?.isActive == true) cancelDownload() else download() }
        checkButton = Button(this).apply { text = "Check again" }
        checkButton.setOnClickListener { refresh() }
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
                addView(checkButton)
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
        pollJob?.cancel()
        downloadButton.visibility = android.view.View.GONE
        runButton.isEnabled = false
        status.text = "Checking Gemini Nano..."
        pollJob =
            scope.launch {
                val availability =
                    try {
                        MlKitBackend(config).availability()
                    } catch (e: Exception) {
                        BackendAvailability.Unavailable("could not be checked (${e::class.simpleName}: ${e.message})")
                    }
                runOnUiThread { show(availability) }
                if (availability == BackendAvailability.Downloading) {
                    delay(POLL_MILLIS)
                    runOnUiThread { refresh() }
                }
            }
    }

    private fun show(availability: BackendAvailability) {
        if (downloadJob?.isActive == true) return
        status.text = describe(availability, config.release)
        runButton.isEnabled = availability == BackendAvailability.Available
        downloadButton.text = "Download model"
        downloadButton.visibility =
            if (availability == BackendAvailability.Downloadable) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun download() {
        downloadButton.text = "Cancel download"
        downloadJob =
            scope.launch {
                var done = false
                MlKitBackend(config).download().collect { step ->
                    done = step == ModelDownload.Completed || step is ModelDownload.Failed
                    runOnUiThread {
                        status.text =
                            when (step) {
                                is ModelDownload.Started -> {
                                    "Downloading ${step.bytesToDownload / MEGABYTE} MB..."
                                }

                                is ModelDownload.Progress -> {
                                    "Downloaded ${step.bytesDownloaded / MEGABYTE} MB"
                                }

                                ModelDownload.Completed -> {
                                    "Download finished, checking the model..."
                                }

                                is ModelDownload.Failed -> {
                                    listOfNotNull("Download failed: ${step.reason}", hintFor(step.reason))
                                        .joinToString("\n")
                                }
                            }
                    }
                }
                if (done) runOnUiThread { downloadButton.text = "Download model" }
                downloadJob = null
                if (done && !status.text.startsWith("Download failed")) runOnUiThread { refresh() }
            }
    }

    private fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        downloadButton.text = "Download model"
        status.text = "Download cancelled. It may continue in AICore; tap Check again."
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
                val backend = ForegroundBackend(ObservingBackend(MlKitBackend(config), ::showProgress), ::resumedNow)
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

    private fun showProgress(text: String) = runOnUiThread { status.text = text }

    private fun append(line: String) = runOnUiThread { output.append(line + "\n") }
}

private const val MEGABYTE = 1_000_000L
private const val POLL_MILLIS = 5_000L

/** Tells the screen which request is running and when AICore pushes back, so a slow run does not look frozen. */
private class ObservingBackend(
    private val delegate: ModelBackend,
    private val report: (String) -> Unit,
) : ModelBackend by delegate {
    private var requests = 0

    override suspend fun generate(
        prompt: String,
        sampling: Sampling,
    ): String {
        report("Request ${++requests} sent to Gemini Nano...")
        try {
            return delegate.generate(prompt, sampling)
        } catch (e: BackendException.QuotaExceeded) {
            report("AICore quota reached. Waiting before request $requests is retried.")
            throw e
        }
    }
}
