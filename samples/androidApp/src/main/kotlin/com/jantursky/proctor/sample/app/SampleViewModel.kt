package com.jantursky.proctor.sample.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.jantursky.proctor.sample.ProgressBackend
import com.jantursky.proctor.sample.SampleRow
import com.jantursky.proctor.sample.parseSampleRow
import com.jantursky.proctor.sample.sampleRequestCount
import com.jantursky.proctor.sample.sampleSuites
import com.jantursky.proctor.sample.toSampleRow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The screens of the sample app; [SampleViewModel.screen] is the one on top. */
sealed interface Screen {
    data object Home : Screen

    data object History : Screen

    data class Detail(
        val row: SampleRow,
    ) : Screen
}

enum class Tone { Neutral, Good, Warn, Bad }

/** What the model card shows: a headline, advice or detail under it, and a colour for the dot. */
data class ModelStatus(
    val title: String,
    val detail: String? = null,
    val tone: Tone = Tone.Neutral,
    val checking: Boolean = false,
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

    /** Finished requests and the total this run will send; `total` is 0 when no run is active. */
    var done by mutableStateOf(0)
        private set
    var total by mutableStateOf(0)
        private set
    var waiting by mutableStateOf(false)
        private set
    var remainingSeconds by mutableStateOf<Long?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var resultsDirectory by mutableStateOf<String?>(null)
        private set
    val rows = mutableStateListOf<SampleRow>()

    /** Runs saved by earlier sessions, newest first. */
    val history = mutableStateListOf<SampleRow>()
    private val stack = mutableStateListOf<Screen>(Screen.Home)
    val screen: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun open(screen: Screen) {
        if (screen == Screen.History) loadHistory()
        stack += screen
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    /** Reads every result file the app has written. A file that cannot be read is skipped. */
    fun loadHistory() {
        val directory = ResultStore.forApp(getApplication()).directory
        val files = directory.listFiles { file -> file.name.endsWith(".proctor.json") }.orEmpty()
        val parsed = files.mapNotNull { parseSampleRow(it.readText()) }.sortedByDescending { it.startedAt }
        history.clear()
        history.addAll(parsed)
    }

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
        done = 0
        total = sampleRequestCount
        waiting = false
        remainingSeconds = null
        val startedAt = System.nanoTime()
        val store = ResultStore.forApp(getApplication())
        resultsDirectory = store.directory.path
        runJob =
            viewModelScope.launch {
                val counting =
                    ProgressBackend(
                        MlKitBackend(config),
                        sampleRequestCount,
                        onProgress = { finished, all ->
                            done = finished
                            waiting = false
                            val elapsed = (System.nanoTime() - startedAt) / NANOS_PER_SECOND
                            remainingSeconds = if (finished > 0) elapsed * (all - finished) / finished else null
                        },
                        onWaiting = { waiting = true },
                    )
                val backend = ForegroundBackend(counting, { resumed })
                try {
                    for (suite in sampleSuites) {
                        // One prompt version at a time, so each row shows up as soon as its run finishes.
                        for (prompt in suite.prompts) {
                            val result = EvalRunner(backend).run(suite, prompt.version)
                            store.write(result)
                            rows += result.toSampleRow()
                        }
                    }
                    message = "Done. ${rows.size} result files written."
                } catch (e: RunAbortedException) {
                    message = "Run aborted. ${e.message}"
                } finally {
                    running = false
                    total = 0
                    remainingSeconds = null
                }
            }
    }
}

private const val MEGABYTE = 1_000_000L
private const val POLL_MILLIS = 5_000L
private const val NANOS_PER_SECOND = 1_000_000_000L
