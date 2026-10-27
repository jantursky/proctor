package com.jantursky.proctor.sample.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Tone { Neutral, Good, Warn, Bad }

/** What the model card shows: a headline, advice or detail under it, and a colour for the dot. */
data class ModelStatus(
    val title: String,
    val detail: String? = null,
    val tone: Tone = Tone.Neutral,
    val checking: Boolean = false,
)

/** One finished run, as a row in the results list. */
data class RunRow(
    val suite: String,
    val promptVersion: String,
    val passed: Int,
    val failed: Int,
    val notGraded: Int,
    val total: Int,
    val meanScore: Double?,
    val suitePassed: Boolean,
)

class SampleViewModel(
    application: Application,
) : AndroidViewModel(application) {
    var config by mutableStateOf(MlKitConfig())
        private set
    var status by mutableStateOf(ModelStatus("Checking Gemini Nano", checking = true))
        private set
    var canDownload by mutableStateOf(false)
        private set
    var downloading by mutableStateOf(false)
        private set
    var canRun by mutableStateOf(false)
        private set
    var running by mutableStateOf(false)
        private set
    var progress by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var resultsDirectory by mutableStateOf<String?>(null)
        private set
    val rows = mutableStateListOf<RunRow>()

    /** Set by the activity: AICore serves only the app that is on screen. */
    @Volatile
    var resumed = false

    private var checkJob: Job? = null
    private var downloadJob: Job? = null
    private var runJob: Job? = null

    init {
        check()
    }

    fun select(
        release: NanoRelease = config.release,
        preference: NanoPreference = config.preference,
    ) {
        if (running || downloading) return
        config = MlKitConfig(release, preference)
        check()
    }

    fun check() {
        checkJob?.cancel()
        canDownload = false
        canRun = false
        status = ModelStatus("Checking Gemini Nano", checking = true)
        checkJob =
            viewModelScope.launch {
                val availability =
                    try {
                        MlKitBackend(config).availability()
                    } catch (e: Exception) {
                        BackendAvailability.Unavailable("could not be checked (${e::class.simpleName}: ${e.message})")
                    }
                show(availability)
                if (availability == BackendAvailability.Downloading) {
                    delay(POLL_MILLIS)
                    check()
                }
            }
    }

    private fun show(availability: BackendAvailability) {
        val release = config.release
        status =
            when (availability) {
                BackendAvailability.Available -> {
                    ModelStatus("Available", "Ready to run.", Tone.Good)
                }

                BackendAvailability.Downloadable -> {
                    ModelStatus(
                        "Not downloaded",
                        "This device supports the model. Download it to run suites.",
                        Tone.Warn,
                    )
                }

                BackendAvailability.Downloading -> {
                    ModelStatus("Downloading", "Checking again every few seconds.", Tone.Warn)
                }

                is BackendAvailability.Unavailable -> {
                    ModelStatus(
                        "Unavailable",
                        listOfNotNull(
                            availability.reason,
                            hintFor(availability.reason),
                            PREVIEW_HINT.takeIf { release == NanoRelease.Preview },
                        ).joinToString("\n\n"),
                        Tone.Bad,
                    )
                }
            }
        canRun = availability == BackendAvailability.Available
        canDownload = availability == BackendAvailability.Downloadable
    }

    fun download() {
        if (downloading) return
        downloading = true
        downloadJob =
            viewModelScope.launch {
                var finished = false
                MlKitBackend(config).download().collect { step ->
                    when (step) {
                        is ModelDownload.Started -> {
                            status =
                                ModelStatus("Downloading", "${step.bytesToDownload / MEGABYTE} MB to fetch.", Tone.Warn)
                        }

                        is ModelDownload.Progress -> {
                            status =
                                ModelStatus(
                                    "Downloading",
                                    "${step.bytesDownloaded / MEGABYTE} MB downloaded.",
                                    Tone.Warn,
                                )
                        }

                        ModelDownload.Completed -> {
                            finished = true
                        }

                        is ModelDownload.Failed -> {
                            status =
                                ModelStatus(
                                    "Download failed",
                                    listOfNotNull(step.reason, hintFor(step.reason)).joinToString("\n\n"),
                                    Tone.Bad,
                                )
                        }
                    }
                }
                downloading = false
                if (finished) check()
            }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloading = false
        status = ModelStatus("Download cancelled", "AICore may keep downloading. Check again in a moment.", Tone.Warn)
        canDownload = true
    }

    fun run() {
        if (running) return
        running = true
        rows.clear()
        message = null
        val store = ResultStore.forApp(getApplication())
        resultsDirectory = store.directory.path
        runJob =
            viewModelScope.launch {
                val backend = ForegroundBackend(Observing(MlKitBackend(config)) { progress = it }, { resumed })
                try {
                    for (suite in sampleSuites) {
                        for (result in EvalRunner(backend).runAll(suite)) {
                            store.write(result)
                            val s = result.summary
                            rows +=
                                RunRow(
                                    suite.name,
                                    result.promptVersion,
                                    s.passed,
                                    s.failed,
                                    s.notGraded,
                                    s.total,
                                    s.meanScore,
                                    s.suitePassed,
                                )
                        }
                    }
                    message = "Done. ${rows.size} result files written."
                } catch (e: RunAbortedException) {
                    message = "Run aborted. ${e.message}"
                } finally {
                    running = false
                    progress = null
                }
            }
    }
}

/** Reports which request is running and when AICore pushes back, so a slow run does not look frozen. */
private class Observing(
    private val delegate: ModelBackend,
    private val report: (String) -> Unit,
) : ModelBackend by delegate {
    private var requests = 0

    override suspend fun generate(
        prompt: String,
        sampling: Sampling,
    ): String {
        report("Request ${++requests} sent to Gemini Nano")
        try {
            return delegate.generate(prompt, sampling)
        } catch (e: BackendException.QuotaExceeded) {
            report("AICore quota reached. Waiting before the next try.")
            throw e
        }
    }
}

private const val MEGABYTE = 1_000_000L
private const val POLL_MILLIS = 5_000L
