package com.jantursky.proctor.ios

import com.jantursky.proctor.BackendAvailability
import com.jantursky.proctor.BackendException
import com.jantursky.proctor.Environment
import com.jantursky.proctor.ModelBackend
import com.jantursky.proctor.Sampling
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Apple Foundation Models through [bridge]. Needs iOS 26 or newer on an Apple Intelligence device, or a simulator
 * whose Mac host has Apple Intelligence.
 *
 * Apple exposes no model version. The model changes with the OS (26.0 to 26.3, 26.4 and 27.0 each ship a
 * different one), so [Environment.modelVersion] is derived from the OS version and marked as such. A simulator
 * borrows the model of its Mac host, so there it records the host's Darwin release too.
 */
public class FoundationModelsBackend internal constructor(
    private val bridge: FoundationModelsBridge,
    private val device: DeviceInfo,
) : ModelBackend {
    public constructor(bridge: FoundationModelsBridge) : this(bridge, DeviceInfo.current())

    override suspend fun availability(): BackendAvailability = bridge.availability().toAvailability()

    override suspend fun environment(): Environment {
        val host = device.kernelRelease?.let { "host Darwin $it" } ?: "unknown host"
        return Environment(
            platform = if (device.simulator) "ios-simulator" else "ios",
            deviceModel = device.model,
            // In a simulator the process runs against the Mac's kernel, and so does the model.
            osVersion = if (device.simulator) "${device.osVersion}, simulator on $host" else device.osVersion,
            modelVersion =
                if (device.simulator) {
                    "iOS ${device.systemVersion} sim on ${host.removePrefix("host ")} (os-derived)"
                } else {
                    "iOS ${device.systemVersion} (os-derived)"
                },
            backend = "foundation-models",
        )
    }

    override suspend fun generate(
        prompt: String,
        sampling: Sampling,
    ): String =
        suspendCancellableCoroutine { continuation ->
            val request =
                BridgeRequest(
                    prompt,
                    sampling.temperature,
                    sampling.topK,
                    sampling.seed?.toLong(),
                    sampling.maxOutputTokens,
                )
            bridge.generate(
                request,
                object : BridgeCompletion {
                    override fun succeed(text: String) {
                        if (text.isBlank()) {
                            continuation.resumeWithException(BackendException.Failed("The model returned no text"))
                        } else {
                            continuation.resume(text)
                        }
                    }

                    override fun fail(
                        kind: BridgeErrorKind,
                        message: String,
                    ) {
                        continuation.resumeWithException(kind.toException(message))
                    }
                },
            )
        }
}

internal fun BridgeAvailability.toAvailability(): BackendAvailability =
    when (this) {
        BridgeAvailability.AVAILABLE -> {
            BackendAvailability.Available
        }

        // The model assets are still being prepared or downloaded.
        BridgeAvailability.MODEL_NOT_READY -> {
            BackendAvailability.Downloading
        }

        BridgeAvailability.DEVICE_NOT_ELIGIBLE -> {
            BackendAvailability.Unavailable(
                "This device is not eligible for Apple Intelligence",
            )
        }

        BridgeAvailability.APPLE_INTELLIGENCE_NOT_ENABLED -> {
            BackendAvailability.Unavailable(
                "Apple Intelligence is not enabled in Settings",
            )
        }
    }

/**
 * Rate limiting and concurrent requests ask the caller to wait. A model that cannot load is unavailable. Guardrail,
 * refusal, context and language errors are the model's answer to this input: the case is recorded as not graded,
 * with the reason, and the report lists it.
 */
internal fun BridgeErrorKind.toException(message: String): BackendException =
    when (this) {
        BridgeErrorKind.RATE_LIMITED, BridgeErrorKind.CONCURRENT_REQUESTS -> BackendException.QuotaExceeded()

        BridgeErrorKind.ASSETS_UNAVAILABLE -> BackendException.Unavailable(message)

        BridgeErrorKind.EXCEEDED_CONTEXT_WINDOW_SIZE,
        BridgeErrorKind.GUARDRAIL_VIOLATION,
        BridgeErrorKind.REFUSAL,
        BridgeErrorKind.UNSUPPORTED_LANGUAGE_OR_LOCALE,
        BridgeErrorKind.OTHER,
        -> BackendException.Failed("Foundation Models ${name.lowercase().replace('_', ' ')}: $message")
    }
