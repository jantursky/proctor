package com.jantursky.proctor.android

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.ModelReleaseStage
import com.jantursky.proctor.BackendAvailability
import com.jantursky.proctor.Sampling
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private class NamedClient(
    private val name: String,
) : PromptClient {
    override suspend fun checkStatus() = FeatureStatus.AVAILABLE

    override suspend fun baseModelName() = name

    override suspend fun warmup() = Unit

    override fun download(): Flow<com.google.mlkit.genai.common.DownloadStatus> = emptyFlow()

    override suspend fun generate(
        prompt: String,
        sampling: Sampling,
    ): String? = "reply"
}

/** One case per model the AICore picker offers: Gemini Nano 4 Full and Fast, each as stable and as preview. */
class ModelConfigTest {
    private val models =
        listOf(
            Triple(MlKitConfig(NanoRelease.Stable, NanoPreference.Full), "stable/full", "nano-v4-full"),
            Triple(MlKitConfig(NanoRelease.Stable, NanoPreference.Fast), "stable/fast", "nano-v4-fast"),
            Triple(MlKitConfig(NanoRelease.Preview, NanoPreference.Full), "preview/full", "nano-v4-full"),
            Triple(MlKitConfig(NanoRelease.Preview, NanoPreference.Fast), "preview/fast", "nano-v4-fast"),
        )

    @Test
    fun everyModelHasItsOwnBackendLabel() {
        models.forEach { (config, label, _) -> assertEquals("mlkit-genai-prompt $label", config.describe()) }
        assertEquals(models.size, models.map { it.first.describe() }.toSet().size)
    }

    @Test
    fun theDefaultIsTheStableFullModel() {
        assertEquals("mlkit-genai-prompt stable/full", MlKitConfig().describe())
        assertEquals("mlkit-genai-prompt preview/fast", MlKitConfig.PreviewFast.describe())
        assertEquals("mlkit-genai-prompt preview/full", MlKitConfig.PreviewFull.describe())
    }

    @Test
    fun theConfigurationMapsToTheMatchingMlKitSettings() {
        val stages = models.associate { it.first to it.first.modelReleaseStage() }
        val preferences = models.associate { it.first to it.first.modelPreference() }
        models.forEach { (config, _, _) ->
            val preview = config.release == NanoRelease.Preview
            assertEquals(if (preview) ModelReleaseStage.PREVIEW else ModelReleaseStage.STABLE, stages.getValue(config))
            val fast = config.preference == NanoPreference.Fast
            assertEquals(if (fast) ModelPreference.FAST else ModelPreference.FULL, preferences.getValue(config))
        }
        assertNotEquals(ModelReleaseStage.PREVIEW, ModelReleaseStage.STABLE)
        assertNotEquals(ModelPreference.FAST, ModelPreference.FULL)
    }

    @Test
    fun aRunRecordsTheBackendLabelAndTheModelVersionOfEachModel() =
        runTest {
            val device = DeviceInfo("Google Pixel 11 Pro", "Android 17 (API 37, build X)")
            models.forEach { (config, label, version) ->
                val backend = MlKitBackend(config, NamedClient(version), device)
                val environment = backend.environment()
                assertEquals("mlkit-genai-prompt $label", environment.backend)
                assertEquals(version, environment.modelVersion)
                assertEquals(BackendAvailability.Available, backend.availability())
            }
        }
}
