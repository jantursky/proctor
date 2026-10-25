package com.jantursky.proctor.android

import com.google.mlkit.genai.common.GenAiException
import com.jantursky.proctor.BackendAvailability
import com.jantursky.proctor.BackendException
import com.jantursky.proctor.Environment
import com.jantursky.proctor.ModelBackend
import com.jantursky.proctor.Sampling
import kotlinx.coroutines.CancellationException

/**
 * Gemini Nano through the ML Kit GenAI Prompt API (`com.google.mlkit:genai-prompt`), called directly so the run sees
 * the model name, the preview configuration and AICore's own quota and foreground errors.
 *
 * The Prompt API needs a supported device with a locked bootloader. AICore serves only the top foreground app, so run
 * suites from a foreground activity, see [ProctorHostActivity] and [ForegroundBackend].
 */
public class MlKitBackend internal constructor(
    private val config: MlKitConfig,
    private val client: PromptClient,
    private val device: DeviceInfo,
) : ModelBackend {
    public constructor(
        config: MlKitConfig = MlKitConfig(),
    ) : this(config, RealPromptClient(config), DeviceInfo.current())

    /**
     * Reports the model state. An available model is warmed up here, so the first case does not pay the model load
     * (about a minute on a preview model) in its latency or time out.
     */
    override suspend fun availability(): BackendAvailability {
        val availability =
            try {
                client.checkStatus().toAvailability()
            } catch (e: GenAiException) {
                return BackendAvailability.Unavailable(e.describe())
            }
        if (availability == BackendAvailability.Available) {
            try {
                client.warmup()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Warm-up is an optimization; generation reports a real failure.
            }
        }
        return availability
    }

    override suspend fun environment(): Environment =
        Environment(
            platform = "android",
            deviceModel = device.model,
            osVersion = device.osVersion,
            modelVersion = baseModelNameOrNull(),
            backend = config.describe(),
        )

    override suspend fun generate(
        prompt: String,
        sampling: Sampling,
    ): String {
        try {
            return client.generate(prompt, sampling)?.takeIf { it.isNotBlank() }
                ?: throw BackendException.Failed("The model returned no text")
        } catch (e: GenAiException) {
            throw e.toBackendException()
        } catch (e: CancellationException) {
            throw e
        } catch (e: BackendException) {
            throw e
        } catch (e: Exception) {
            throw BackendException.Failed("ML Kit call failed: ${e.message ?: e::class.simpleName}", e)
        }
    }

    /** `getBaseModelName()` fails while the model is missing; the run then records no version. */
    private suspend fun baseModelNameOrNull(): String? =
        try {
            client.baseModelName().takeIf { it.isNotBlank() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
}
