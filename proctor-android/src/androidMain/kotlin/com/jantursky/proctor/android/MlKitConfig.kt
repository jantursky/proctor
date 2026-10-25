package com.jantursky.proctor.android

/** Which Gemini Nano release the client asks AICore for. */
public enum class NanoRelease { Stable, Preview }

/** `Fast` and `Full` are the two variants of Gemini Nano 4; stable models only offer `Full`. */
public enum class NanoPreference { Full, Fast }

/**
 * The model configuration passed to the ML Kit Prompt API. The default is the stable model; [Preview] asks for a
 * model from the AICore Developer Preview. The backend never falls back to the stable model on its own, because a
 * result tagged preview must come from the preview.
 */
public data class MlKitConfig(
    val release: NanoRelease = NanoRelease.Stable,
    val preference: NanoPreference = NanoPreference.Full,
) {
    /** The text recorded as `Environment.backend`, for example `mlkit-genai-prompt preview/fast`. */
    public fun describe(): String = "mlkit-genai-prompt ${release.name.lowercase()}/${preference.name.lowercase()}"

    public companion object {
        public val PreviewFast: MlKitConfig = MlKitConfig(NanoRelease.Preview, NanoPreference.Fast)
        public val PreviewFull: MlKitConfig = MlKitConfig(NanoRelease.Preview, NanoPreference.Full)
    }
}
