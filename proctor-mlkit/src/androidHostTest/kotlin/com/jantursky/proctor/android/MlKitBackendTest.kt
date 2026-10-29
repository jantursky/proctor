package com.jantursky.proctor.android

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.common.GenAiException.ErrorCode
import com.jantursky.proctor.BackendAvailability
import com.jantursky.proctor.BackendException
import com.jantursky.proctor.Sampling
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

internal fun aiError(
    code: Int,
    message: String = "boom",
    retry: Duration = Duration.ZERO,
) = GenAiException(message, RuntimeException("cause"), code, retry)

private class FakeClient(
    var status: () -> Int = { FeatureStatus.AVAILABLE },
    var name: () -> String = { "nano-v3" },
    var warmup: () -> Unit = {},
    var reply: (String) -> String? = { "reply to $it" },
    var download: () -> Flow<DownloadStatus> = { flow {} },
) : PromptClient {
    var warmups = 0
    val samplings = mutableListOf<Sampling>()

    override suspend fun checkStatus() = status()

    override suspend fun baseModelName() = name()

    override suspend fun warmup() {
        warmups++
        warmup.invoke()
    }

    override fun download() = download.invoke()

    override suspend fun generate(
        prompt: String,
        sampling: Sampling,
    ): String? {
        samplings += sampling
        return reply(prompt)
    }
}

private val device = DeviceInfo("samsung Galaxy S26", "Android 16 (API 36, build X)")

private fun backend(
    client: FakeClient,
    config: MlKitConfig = MlKitConfig(),
) = MlKitBackend(config, client, device)

class MlKitBackendTest {
    @Test
    fun downloadReportsEveryStepUntilCompleted() =
        runTest {
            val client =
                FakeClient(
                    download = {
                        flow {
                            emit(DownloadStatus.DownloadStarted(100))
                            emit(DownloadStatus.DownloadProgress(40))
                            emit(DownloadStatus.DownloadCompleted)
                        }
                    },
                )
            assertEquals(
                listOf(ModelDownload.Started(100), ModelDownload.Progress(40), ModelDownload.Completed),
                backend(client).download().toList(),
            )
        }

    @Test
    fun aFailedDownloadIsReportedNotThrown() =
        runTest {
            val failure = aiError(ErrorCode.NOT_ENOUGH_DISK_SPACE, "full")
            val client = FakeClient(download = { flow { emit(DownloadStatus.DownloadFailed(failure)) } })
            val steps = backend(client).download().toList()
            assertTrue(steps.single() is ModelDownload.Failed)
        }

    @Test
    fun aDownloadThatThrowsAnAiErrorEndsInFailed() =
        runTest {
            val client = FakeClient(download = { flow { throw aiError(ErrorCode.BUSY) } })
            assertTrue(backend(client).download().toList().single() is ModelDownload.Failed)
        }

    @Test
    fun otherDownloadErrorsPropagate() =
        runTest {
            val client = FakeClient(download = { flow { throw IllegalStateException("bug") } })
            assertFailsWith<IllegalStateException> { backend(client).download().toList() }
        }

    @Test
    fun anAvailableModelIsWarmedUp() =
        runTest {
            val client = FakeClient()
            assertEquals(BackendAvailability.Available, backend(client).availability())
            assertEquals(1, client.warmups)
        }

    @Test
    fun aFailedWarmupDoesNotHideAnAvailableModel() =
        runTest {
            val client = FakeClient(warmup = { error("slow") })
            assertEquals(BackendAvailability.Available, backend(client).availability())
        }

    @Test
    fun cancellationDuringWarmupPropagates() =
        runTest {
            val client = FakeClient(warmup = { throw CancellationException("stop") })
            assertFailsWith<CancellationException> { backend(client).availability() }
        }

    @Test
    fun otherStatesAreReportedWithoutWarmingUp() =
        runTest {
            for ((status, expected) in listOf(
                FeatureStatus.DOWNLOADABLE to BackendAvailability.Downloadable,
                FeatureStatus.DOWNLOADING to BackendAvailability.Downloading,
            )) {
                val client = FakeClient(status = { status })
                assertEquals(expected, backend(client).availability())
                assertEquals(0, client.warmups)
            }
            val unavailable = backend(FakeClient(status = { FeatureStatus.UNAVAILABLE })).availability()
            assertTrue((unavailable as BackendAvailability.Unavailable).reason.contains("not supported on this device"))
            val unknown = backend(FakeClient(status = { 42 })).availability()
            assertEquals(BackendAvailability.Unavailable("Unknown ML Kit feature status 42"), unknown)
        }

    @Test
    fun anErrorWhileCheckingTheStatusIsReportedAsUnavailable() =
        runTest {
            val client = FakeClient(status = { throw aiError(606, "FEATURE_NOT_FOUND") })
            val availability = backend(client).availability() as BackendAvailability.Unavailable
            assertEquals("ERROR (606): FEATURE_NOT_FOUND", availability.reason)
        }

    @Test
    fun theEnvironmentCarriesDeviceModelVersionAndConfiguration() =
        runTest {
            val environment = backend(FakeClient(), MlKitConfig.PreviewFast).environment()

            assertEquals("android", environment.platform)
            assertEquals("samsung Galaxy S26", environment.deviceModel)
            assertEquals("Android 16 (API 36, build X)", environment.osVersion)
            assertEquals("nano-v3", environment.modelVersion)
            assertEquals("mlkit-genai-prompt preview/fast", environment.backend)
        }

    @Test
    fun aMissingModelNameIsRecordedAsUnknown() =
        runTest {
            assertNull(backend(FakeClient(name = { "  " })).environment().modelVersion)
            assertNull(backend(FakeClient(name = { throw aiError(606) })).environment().modelVersion)
            assertFailsWith<CancellationException> {
                backend(FakeClient(name = { throw CancellationException("stop") })).environment()
            }
        }

    @Test
    fun generateReturnsTheTextAndPassesSampling() =
        runTest {
            val client = FakeClient()
            val sampling = Sampling(temperature = 0.1, topK = 4)
            assertEquals("reply to hi", backend(client).generate("hi", sampling))
            assertEquals(listOf(sampling), client.samplings)
        }

    @Test
    fun emptyRepliesAreBackendErrors() =
        runTest {
            for (reply in listOf<String?>(null, "", "  ")) {
                val error =
                    assertFailsWith<BackendException.Failed> {
                        backend(
                            FakeClient(reply = { reply }),
                        ).generate("x", Sampling())
                    }
                assertEquals("The model returned no text", error.message)
            }
        }

    @Test
    fun aiCoreErrorsBecomeBackendExceptions() =
        runTest {
            val busy = backend(FakeClient(reply = { throw aiError(ErrorCode.BUSY, retry = Duration.ofSeconds(7)) }))
            assertEquals(
                7.seconds,
                assertFailsWith<BackendException.QuotaExceeded> { busy.generate("x", Sampling()) }.retryAfter,
            )

            val background = backend(FakeClient(reply = { throw aiError(ErrorCode.BACKGROUND_USE_BLOCKED) }))
            assertFailsWith<BackendException.Backgrounded> { background.generate("x", Sampling()) }
        }

    @Test
    fun otherFailuresAreNotesNotCrashes() =
        runTest {
            val runtime = backend(FakeClient(reply = { throw IllegalStateException("binder died") }))
            assertTrue(
                assertFailsWith<BackendException.Failed> {
                    runtime.generate("x", Sampling())
                }.message!!.contains("binder died"),
            )

            val silent = backend(FakeClient(reply = { throw object : Exception() {} }))
            assertTrue(
                assertFailsWith<BackendException.Failed> {
                    silent.generate("x", Sampling())
                }.message!!.contains("ML Kit call failed"),
            )

            val cancelled = backend(FakeClient(reply = { throw CancellationException("stop") }))
            assertFailsWith<CancellationException> { cancelled.generate("x", Sampling()) }
        }

    @Test
    fun theDeviceInfoReadsBuildFields() {
        val info = DeviceInfo.current()
        assertTrue(info.osVersion.startsWith("Android "))
    }

    @Test
    fun configurationsDescribeThemselves() {
        assertEquals("mlkit-genai-prompt stable/full", MlKitConfig().describe())
        assertEquals("mlkit-genai-prompt preview/full", MlKitConfig.PreviewFull.describe())
        assertEquals(NanoRelease.Preview, MlKitConfig.PreviewFast.release)
    }
}
