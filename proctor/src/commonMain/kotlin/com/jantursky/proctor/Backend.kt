package com.jantursky.proctor

import kotlinx.serialization.Serializable

/**
 * A language model the suite runs against. Adapters implement this for Gemini Nano, Apple Foundation Models
 * or a desktop model; the core never depends on any of them.
 */
public interface ModelBackend {
    /** Whether the model can serve requests now. A run reports anything but [BackendAvailability.Available]. */
    public suspend fun availability(): BackendAvailability

    /** Facts about the device and model that every result is tagged with. */
    public suspend fun environment(): Environment

    /**
     * Returns the model's reply to [prompt].
     *
     * @throws BackendException for platform limits the runner handles: [BackendException.QuotaExceeded],
     * [BackendException.Backgrounded], [BackendException.Unavailable] and [BackendException.Failed].
     */
    public suspend fun generate(
        prompt: String,
        sampling: Sampling,
    ): String
}

/** The state of the system model before a run starts. */
public sealed interface BackendAvailability {
    public data object Available : BackendAvailability

    /** Supported, but the model has not been fetched yet. */
    public data object Downloadable : BackendAvailability

    public data object Downloading : BackendAvailability

    public data class Unavailable(
        val reason: String,
    ) : BackendAvailability
}

/**
 * Where a run happened. [modelVersion] is the platform's own identifier (`getBaseModelName()` on Android, the OS
 * build on iOS) and is `null` when the platform does not expose one.
 */
@Serializable
public data class Environment(
    val platform: String,
    val deviceModel: String,
    val osVersion: String,
    val modelVersion: String? = null,
    val backend: String,
)

/** Sampling settings, recorded in every result. A `null` value means the platform default was used. */
@Serializable
public data class Sampling(
    val temperature: Double? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null,
    val seed: Int? = null,
)

/** Platform limits a backend reports. None of them is a verdict on the prompt. */
public sealed class BackendException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** The per-app inference quota is used up. [retryAfter] is the platform's hint, if it gives one. */
    public class QuotaExceeded(
        public val retryAfter: kotlin.time.Duration? = null,
        cause: Throwable? = null,
    ) : BackendException("Inference quota exceeded", cause)

    /** The app lost the foreground; AICore only serves the top foreground app. */
    public class Backgrounded(
        cause: Throwable? = null,
    ) : BackendException("The app is not in the foreground", cause)

    public class Unavailable(
        public val reason: String,
        cause: Throwable? = null,
    ) : BackendException("Model unavailable: $reason", cause)

    public class Failed(
        message: String,
        cause: Throwable? = null,
    ) : BackendException(message, cause)
}
