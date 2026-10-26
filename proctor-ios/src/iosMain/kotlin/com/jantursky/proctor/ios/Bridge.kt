package com.jantursky.proctor.ios

/**
 * What Swift implements. FoundationModels is Swift-only, so Kotlin cannot call it; a small Swift class (see
 * `ios-bridge/ProctorFoundationModelsBridge.swift`) conforms to this protocol and the app hands it to
 * [FoundationModelsBackend].
 */
public interface FoundationModelsBridge {
    /** `SystemLanguageModel.default.availability`, translated. Synchronous in Swift too. */
    public fun availability(): BridgeAvailability

    /** Starts a generation and reports through [completion] exactly once. */
    public fun generate(
        request: BridgeRequest,
        completion: BridgeCompletion,
    )
}

/** One prompt with its sampling settings. A `null` means the platform default. */
public class BridgeRequest(
    public val prompt: String,
    public val temperature: Double?,
    public val topK: Int?,
    public val seed: Long?,
    public val maxOutputTokens: Int?,
)

/** Where Swift reports a finished generation. */
public interface BridgeCompletion {
    public fun succeed(text: String)

    public fun fail(
        kind: BridgeErrorKind,
        message: String,
    )
}

/** The cases of `SystemLanguageModel.Availability`, flattened. */
public enum class BridgeAvailability {
    AVAILABLE,
    DEVICE_NOT_ELIGIBLE,
    APPLE_INTELLIGENCE_NOT_ENABLED,
    MODEL_NOT_READY,
}

/** The cases of `LanguageModelSession.GenerationError`, grouped by what the runner does with them. */
public enum class BridgeErrorKind {
    RATE_LIMITED,
    CONCURRENT_REQUESTS,
    ASSETS_UNAVAILABLE,
    EXCEEDED_CONTEXT_WINDOW_SIZE,
    GUARDRAIL_VIOLATION,
    REFUSAL,
    UNSUPPORTED_LANGUAGE_OR_LOCALE,
    OTHER,
}
