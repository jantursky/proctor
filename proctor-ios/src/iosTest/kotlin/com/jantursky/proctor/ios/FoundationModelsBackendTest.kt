package com.jantursky.proctor.ios

import com.jantursky.proctor.BackendAvailability
import com.jantursky.proctor.BackendException
import com.jantursky.proctor.Sampling
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeBridge(
    var availability: BridgeAvailability = BridgeAvailability.AVAILABLE,
    var respond: (
        BridgeRequest,
        BridgeCompletion,
    ) -> Unit = { request, completion -> completion.succeed("reply to ${request.prompt}") },
) : FoundationModelsBridge {
    val requests = mutableListOf<BridgeRequest>()

    override fun availability() = availability

    override fun generate(
        request: BridgeRequest,
        completion: BridgeCompletion,
    ) {
        requests += request
        respond(request, completion)
    }
}

class FoundationModelsBackendTest {
    @Test
    fun availabilityMapsEveryCase() =
        runTest {
            val expected =
                mapOf(
                    BridgeAvailability.AVAILABLE to BackendAvailability.Available,
                    BridgeAvailability.MODEL_NOT_READY to BackendAvailability.Downloading,
                    BridgeAvailability.DEVICE_NOT_ELIGIBLE to
                        BackendAvailability.Unavailable("This device is not eligible for Apple Intelligence"),
                    BridgeAvailability.APPLE_INTELLIGENCE_NOT_ENABLED to
                        BackendAvailability.Unavailable("Apple Intelligence is not enabled in Settings"),
                )
            for ((bridgeState, state) in expected) {
                assertEquals(state, FoundationModelsBackend(FakeBridge(availability = bridgeState)).availability())
            }
        }

    @Test
    fun generatePassesPromptAndSamplingAndReturnsTheText() =
        runTest {
            val bridge = FakeBridge()
            val sampling = Sampling(temperature = 0.2, topK = 8, seed = 7, maxOutputTokens = 64)

            assertEquals("reply to hello", FoundationModelsBackend(bridge).generate("hello", sampling))

            val request = bridge.requests.single()
            assertEquals("hello", request.prompt)
            assertEquals(0.2, request.temperature)
            assertEquals(8, request.topK)
            assertEquals(7L, request.seed)
            assertEquals(64, request.maxOutputTokens)
        }

    @Test
    fun unsetSamplingStaysUnset() =
        runTest {
            val bridge = FakeBridge()
            FoundationModelsBackend(bridge).generate("x", Sampling())
            val request = bridge.requests.single()
            assertNull(request.temperature)
            assertNull(request.topK)
            assertNull(request.seed)
            assertNull(request.maxOutputTokens)
        }

    @Test
    fun blankRepliesAreBackendErrors() =
        runTest {
            val bridge = FakeBridge(respond = { _, completion -> completion.succeed("  ") })
            val error =
                assertFailsWith<BackendException.Failed> { FoundationModelsBackend(bridge).generate("x", Sampling()) }
            assertEquals("The model returned no text", error.message)
        }

    @Test
    fun errorKindsBecomeTheMatchingBackendException() =
        runTest {
            suspend fun failure(kind: BridgeErrorKind): BackendException {
                val bridge = FakeBridge(respond = { _, completion -> completion.fail(kind, "details") })
                return assertFailsWith<BackendException> { FoundationModelsBackend(bridge).generate("x", Sampling()) }
            }

            assertTrue(failure(BridgeErrorKind.RATE_LIMITED) is BackendException.QuotaExceeded)
            assertTrue(failure(BridgeErrorKind.CONCURRENT_REQUESTS) is BackendException.QuotaExceeded)
            assertEquals(
                "details",
                (failure(BridgeErrorKind.ASSETS_UNAVAILABLE) as BackendException.Unavailable).reason,
            )
            val failed =
                listOf(
                    BridgeErrorKind.EXCEEDED_CONTEXT_WINDOW_SIZE,
                    BridgeErrorKind.GUARDRAIL_VIOLATION,
                    BridgeErrorKind.REFUSAL,
                    BridgeErrorKind.UNSUPPORTED_LANGUAGE_OR_LOCALE,
                    BridgeErrorKind.OTHER,
                )
            for (kind in failed) {
                val error = failure(kind) as BackendException.Failed
                assertTrue(error.message!!.endsWith(": details"), error.message)
            }
            assertEquals(
                "Foundation Models guardrail violation: details",
                failure(BridgeErrorKind.GUARDRAIL_VIOLATION).message,
            )
        }

    @Test
    fun theEnvironmentNamesThePlatformAndMarksTheModelVersionAsOsDerived() =
        runTest {
            val environment = FoundationModelsBackend(FakeBridge()).environment()

            assertTrue(environment.platform == "ios" || environment.platform == "ios-simulator")
            assertEquals("foundation-models", environment.backend)
            assertTrue(environment.modelVersion!!.endsWith("(os-derived)"))
            assertTrue(environment.deviceModel.isNotBlank())
            assertTrue(environment.osVersion.isNotBlank())
        }

    @Test
    fun aSimulatorRecordsTheMacHostWhereTheModelRuns() =
        runTest {
            val device =
                DeviceInfo(
                    "iPhone19,2",
                    "Version 27.0 (Build 24A434)",
                    "27.0",
                    simulator = true,
                    kernelRelease = "27.0.0",
                )
            val environment = FoundationModelsBackend(FakeBridge(), device).environment()

            assertEquals("ios-simulator", environment.platform)
            assertEquals("Version 27.0 (Build 24A434), simulator on host Darwin 27.0.0", environment.osVersion)
            assertEquals("iOS 27.0 sim on Darwin 27.0.0 (os-derived)", environment.modelVersion)
        }

    @Test
    fun aDeviceRecordsItsOwnOsVersion() =
        runTest {
            val device =
                DeviceInfo(
                    "iPhone17,1",
                    "Version 26.4 (Build 23E100)",
                    "26.4",
                    simulator = false,
                    kernelRelease = null,
                )
            val environment = FoundationModelsBackend(FakeBridge(), device).environment()

            assertEquals("ios", environment.platform)
            assertEquals("Version 26.4 (Build 23E100)", environment.osVersion)
            assertEquals("iOS 26.4 (os-derived)", environment.modelVersion)
            assertEquals("iPhone17,1", environment.deviceModel)
        }
}
