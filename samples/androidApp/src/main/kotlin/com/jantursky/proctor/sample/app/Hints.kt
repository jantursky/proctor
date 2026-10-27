package com.jantursky.proctor.sample.app

import com.jantursky.proctor.BackendAvailability
import com.jantursky.proctor.android.NanoRelease

/** Plain-language advice for the AICore error names that end up in a reason text. */
internal fun hintFor(reason: String): String? =
    when {
        "NOT_ENOUGH_DISK_SPACE" in reason -> {
            "Free some storage and try again."
        }

        "NEEDS_SYSTEM_UPDATE" in reason -> {
            "Update Android in system settings, then check again."
        }

        "AICORE_INCOMPATIBLE" in reason -> {
            "Update the AICore app in Google Play, then check again."
        }

        "NOT_SUPPORTED" in reason || "NOT_AVAILABLE" in reason || "not supported" in reason -> {
            "The Prompt API needs a supported device with a locked bootloader."
        }

        "BUSY" in reason || "QUOTA" in reason -> {
            "AICore is limiting this app. Wait a few minutes and try again."
        }

        "BACKGROUND_USE_BLOCKED" in reason -> {
            "Keep this screen in the foreground."
        }

        else -> {
            null
        }
    }

internal const val PREVIEW_HINT =
    "The preview models need the AICore Developer Preview: join the aicore-experimental group, become a tester, " +
        "update AICore until \"Android AICore (Beta)\" shows up, and pick a preview model in its model picker."

/** The status line for a model state, with advice where there is some. */
internal fun describe(
    availability: BackendAvailability,
    release: NanoRelease,
): String =
    when (availability) {
        BackendAvailability.Available -> {
            "Gemini Nano: available"
        }

        BackendAvailability.Downloadable -> {
            "Gemini Nano: supported, not downloaded yet"
        }

        BackendAvailability.Downloading -> {
            "Gemini Nano: downloading. Checking again every few seconds."
        }

        is BackendAvailability.Unavailable -> {
            listOfNotNull(
                "Gemini Nano: unavailable. ${availability.reason}",
                hintFor(availability.reason),
                PREVIEW_HINT.takeIf { release == NanoRelease.Preview },
            ).joinToString("\n")
        }
    }
