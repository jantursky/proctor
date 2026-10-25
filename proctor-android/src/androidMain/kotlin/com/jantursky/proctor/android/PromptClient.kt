package com.jantursky.proctor.android

import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.ModelReleaseStage
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.modelConfig
import com.jantursky.proctor.Sampling

/** The part of the ML Kit client the backend uses, so the error handling can be tested without AICore. */
internal interface PromptClient {
    /** A `FeatureStatus` constant. */
    suspend fun checkStatus(): Int

    suspend fun baseModelName(): String

    suspend fun warmup()

    /** The first candidate's text, `null` if the model returned none. */
    suspend fun generate(
        prompt: String,
        sampling: Sampling,
    ): String?
}

internal class RealPromptClient(
    config: MlKitConfig,
) : PromptClient {
    private val model: GenerativeModel =
        Generation.getClient(
            generationConfig {
                modelConfig =
                    modelConfig {
                        releaseStage =
                            if (config.release ==
                                NanoRelease.Preview
                            ) {
                                ModelReleaseStage.PREVIEW
                            } else {
                                ModelReleaseStage.STABLE
                            }
                        preference =
                            if (config.preference == NanoPreference.Fast) ModelPreference.FAST else ModelPreference.FULL
                    }
            },
        )

    override suspend fun checkStatus(): Int = model.checkStatus()

    override suspend fun baseModelName(): String = model.getBaseModelName()

    override suspend fun warmup() = model.warmup()

    override suspend fun generate(
        prompt: String,
        sampling: Sampling,
    ): String? {
        val request =
            GenerateContentRequest
                .Builder(TextPart(prompt))
                .apply {
                    sampling.temperature?.let { temperature = it.toFloat() }
                    sampling.topK?.let { topK = it }
                    sampling.seed?.let { seed = it }
                    sampling.maxOutputTokens?.let { maxOutputTokens = it }
                    // Some preview devices reject more than one candidate.
                    candidateCount = 1
                }.build()
        return model
            .generateContent(request)
            .candidates
            .firstOrNull()
            ?.text
    }
}
