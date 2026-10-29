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
import com.jantursky.proctor.RunnerConfig
import com.jantursky.proctor.android.ForegroundBackend
import com.jantursky.proctor.android.MlKitBackend
import com.jantursky.proctor.android.MlKitConfig
import com.jantursky.proctor.android.ModelDownload
import com.jantursky.proctor.android.NanoPreference
import com.jantursky.proctor.android.NanoRelease
import com.jantursky.proctor.android.ResultStore
import com.jantursky.proctor.sample.ProgressBackend
import com.jantursky.proctor.sample.SampleCase
import com.jantursky.proctor.sample.SampleCaseDef
import com.jantursky.proctor.sample.SampleEdits
import com.jantursky.proctor.sample.SampleRow
import com.jantursky.proctor.sample.parseSampleRow
import com.jantursky.proctor.sample.runCustomCase
import com.jantursky.proctor.sample.sampleRequestCount
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

    /** Edit one case of [row] and run it again. */
    data class Edit(
        val row: SampleRow,
        val case: SampleCase,
    ) : Screen

    /** The dataset the next run uses. */
    data object Dataset : Screen

    /** Change one case of the dataset for the next run. */
    data class DatasetCase(
        val case: SampleCaseDef,
    ) : Screen
}

enum class Tone { Neutral, Good, Warn, Bad }

/** What the model card shows: a headline, advice or detail under it, and a colour for the dot. */
data class ModelStatus(
    val title: String,
    val detail: String? = null,
    val tone: Tone = Tone.Neutral,
    val checking: Boolean = false,
    /** Share of a download that is done, or null when the size is not known (shown as an indeterminate bar). */
    val progress: Float? = null,
    val inProgress: Boolean = false,
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

    private val edits = SampleEdits()

    /** The dataset with the user's edits; the list is rebuilt after every change. */
    var cases by mutableStateOf(edits.cases())
        private set
    val editedCount: Int get() = cases.count { it.edited }

    fun saveCase(
        suite: String,
        id: String,
        input: String,
        expected: String,
    ) {
        edits.set(suite, id, input, expected)
        cases = edits.cases()
    }

    fun resetCase(
        suite: String,
        id: String,
    ) {
        edits.reset(suite, id)
        cases = edits.cases()
    }

    fun resetAllCases() {
        edits.resetAll()
        cases = edits.cases()
    }

    /** Runs saved by earlier sessions, newest first. */
    val history = mutableStateListOf<SampleRow>()
    private val stack = mutableStateListOf<Screen>(Screen.Home)
    val screen: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    var customRunning by mutableStateOf(false)
        private set
    var customResult by mutableStateOf<SampleCase?>(null)
        private set
    var customError by mutableStateOf<String?>(null)
        private set

    fun open(screen: Screen) {
        if (screen == Screen.History) loadHistory()
        if (screen is Screen.Edit) {
            customResult = null
            customError = null
        }
        stack += screen
    }

    /** Deletes every saved result file and the lists built from them. Does nothing while a run is active. */
    fun clearHistory() {
        if (running) return
        val directory = ResultStore.forApp(getApplication()).directory
        directory.listFiles { file -> file.name.endsWith(".proctor.json") }.orEmpty().forEach { it.delete() }
        history.clear()
        rows.clear()
        message = null
    }

    /** Runs the user's own version of a case on the selected model, without saving anything. */
    fun runCustom(
        row: SampleRow,
        input: String,
        expected: String,
    ) {
        if (customRunning) return
        customRunning = true
        customResult = null
        customError = null
        viewModelScope.launch {
            try {
                val backend = ForegroundBackend(MlKitBackend(config), { resumed })
                customResult = runCustomCase(backend, row.suite, row.promptVersion, input, expected)
            } catch (e: RunAbortedException) {
                customError = e.message
            } finally {
                customRunning = false
            }
        }
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
                    ModelStatus("Downloading", "AICore is downloading.", Tone.Warn, inProgress = true)
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
                var totalBytes = 0L
                status = ModelStatus("Downloading", "Starting...", Tone.Warn, inProgress = true)
                MlKitBackend(config).download().collect { step ->
                    when (step) {
                        is ModelDownload.Started -> {
                            totalBytes = step.bytesToDownload
                            status =
                                ModelStatus(
                                    "Downloading",
                                    "0 of ${totalBytes / MEGABYTE} MB",
                                    Tone.Warn,
                                    progress = if (totalBytes > 0) 0f else null,
                                    inProgress = true,
                                )
                        }

                        is ModelDownload.Progress -> {
                            val megabytes = step.bytesDownloaded / MEGABYTE
                            val fraction =
                                if (totalBytes > 0) {
                                    (step.bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
                                } else {
                                    null
                                }
                            val detail =
                                if (fraction != null) {
                                    "$megabytes of ${totalBytes / MEGABYTE} MB (${(fraction * 100).toInt()}%)"
                                } else {
                                    "$megabytes MB downloaded"
                                }
                            status =
                                ModelStatus("Downloading", detail, Tone.Warn, progress = fraction, inProgress = true)
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
        val groupId =
            java.util.UUID
                .randomUUID()
                .toString()
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
                val config = RunnerConfig(groupId = groupId)
                try {
                    for (suite in edits.suites()) {
                        // One prompt version at a time, so each row shows up as soon as its run finishes.
                        for (prompt in suite.prompts) {
                            val result = EvalRunner(backend, config).run(suite, prompt.version)
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
