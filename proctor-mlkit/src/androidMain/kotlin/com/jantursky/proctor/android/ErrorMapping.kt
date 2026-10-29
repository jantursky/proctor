package com.jantursky.proctor.android

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.common.GenAiException.ErrorCode
import com.jantursky.proctor.BackendAvailability
import com.jantursky.proctor.BackendException
import kotlin.time.toKotlinDuration

/**
 * Sorts an AICore error into what the runner does with it: wait (quota), stop (foreground), report (model not
 * usable) or note it (anything else). None of them becomes a verdict on the prompt.
 */
internal fun GenAiException.toBackendException(): BackendException =
    when (errorCode) {
        ErrorCode.BUSY, ErrorCode.PER_APP_BATTERY_USE_QUOTA_EXCEEDED -> {
            BackendException.QuotaExceeded(retryDelay.toKotlinDuration().takeIf { it.isPositive() }, this)
        }

        ErrorCode.BACKGROUND_USE_BLOCKED -> {
            BackendException.Backgrounded(this)
        }

        in UNUSABLE_MODEL -> {
            BackendException.Unavailable(describe(), this)
        }

        else -> {
            BackendException.Failed("ML Kit error ${describe()}", this)
        }
    }

internal fun Int.toAvailability(): BackendAvailability =
    when (this) {
        FeatureStatus.AVAILABLE -> {
            BackendAvailability.Available
        }

        FeatureStatus.DOWNLOADABLE -> {
            BackendAvailability.Downloadable
        }

        FeatureStatus.DOWNLOADING -> {
            BackendAvailability.Downloading
        }

        FeatureStatus.UNAVAILABLE -> {
            BackendAvailability.Unavailable(
                "Gemini Nano is not supported on this device or its configuration is not fetched yet",
            )
        }

        else -> {
            BackendAvailability.Unavailable("Unknown ML Kit feature status $this")
        }
    }

internal fun GenAiException.describe(): String = "${errorName(errorCode)} ($errorCode)${message?.let { ": $it" } ?: ""}"

private val UNUSABLE_MODEL =
    setOf(
        ErrorCode.NOT_AVAILABLE,
        ErrorCode.NOT_SUPPORTED,
        ErrorCode.NEEDS_SYSTEM_UPDATE,
        ErrorCode.AICORE_INCOMPATIBLE,
        ErrorCode.NOT_ENOUGH_DISK_SPACE,
    )

private fun errorName(code: Int): String =
    when (code) {
        ErrorCode.UNKNOWN -> "UNKNOWN"
        ErrorCode.REQUEST_PROCESSING_ERROR -> "REQUEST_PROCESSING_ERROR"
        ErrorCode.CANCELLED -> "CANCELLED"
        ErrorCode.NOT_AVAILABLE -> "NOT_AVAILABLE"
        ErrorCode.BUSY -> "BUSY"
        ErrorCode.RESPONSE_PROCESSING_ERROR -> "RESPONSE_PROCESSING_ERROR"
        ErrorCode.REQUEST_TOO_LARGE -> "REQUEST_TOO_LARGE"
        ErrorCode.REQUEST_TOO_SMALL -> "REQUEST_TOO_SMALL"
        ErrorCode.RESPONSE_GENERATION_ERROR -> "RESPONSE_GENERATION_ERROR"
        ErrorCode.NOT_SUPPORTED -> "NOT_SUPPORTED"
        ErrorCode.PER_APP_BATTERY_USE_QUOTA_EXCEEDED -> "PER_APP_BATTERY_USE_QUOTA_EXCEEDED"
        ErrorCode.BACKGROUND_USE_BLOCKED -> "BACKGROUND_USE_BLOCKED"
        ErrorCode.NOT_ENOUGH_DISK_SPACE -> "NOT_ENOUGH_DISK_SPACE"
        ErrorCode.NEEDS_SYSTEM_UPDATE -> "NEEDS_SYSTEM_UPDATE"
        ErrorCode.AICORE_INCOMPATIBLE -> "AICORE_INCOMPATIBLE"
        else -> "ERROR"
    }
