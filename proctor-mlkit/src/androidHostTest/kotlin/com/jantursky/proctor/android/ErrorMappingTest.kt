package com.jantursky.proctor.android

import com.google.mlkit.genai.common.GenAiException.ErrorCode
import com.jantursky.proctor.BackendException
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class ErrorMappingTest {
    @Test
    fun quotaErrorsCarryTheRetryHintWhenThereIsOne() {
        val busy = aiError(ErrorCode.BUSY, retry = Duration.ofMinutes(2)).toBackendException()
        assertEquals(2.minutes, assertIs<BackendException.QuotaExceeded>(busy).retryAfter)

        val daily = aiError(ErrorCode.PER_APP_BATTERY_USE_QUOTA_EXCEEDED).toBackendException()
        assertNull(assertIs<BackendException.QuotaExceeded>(daily).retryAfter)
    }

    @Test
    fun theForegroundErrorIsItsOwnKind() {
        assertIs<BackendException.Backgrounded>(aiError(ErrorCode.BACKGROUND_USE_BLOCKED).toBackendException())
    }

    @Test
    fun errorsAboutTheModelBecomeUnavailable() {
        val codes =
            listOf(
                ErrorCode.NOT_AVAILABLE,
                ErrorCode.NOT_SUPPORTED,
                ErrorCode.NEEDS_SYSTEM_UPDATE,
                ErrorCode.AICORE_INCOMPATIBLE,
                ErrorCode.NOT_ENOUGH_DISK_SPACE,
            )
        for (code in codes) {
            val mapped = aiError(code, "msg").toBackendException()
            assertTrue(assertIs<BackendException.Unavailable>(mapped).reason.endsWith("($code): msg"), "$code")
        }
    }

    @Test
    fun everythingElseIsAFailureNamingTheCode() {
        val names =
            mapOf(
                ErrorCode.UNKNOWN to "UNKNOWN",
                ErrorCode.REQUEST_PROCESSING_ERROR to "REQUEST_PROCESSING_ERROR",
                ErrorCode.CANCELLED to "CANCELLED",
                ErrorCode.RESPONSE_PROCESSING_ERROR to "RESPONSE_PROCESSING_ERROR",
                ErrorCode.REQUEST_TOO_LARGE to "REQUEST_TOO_LARGE",
                ErrorCode.REQUEST_TOO_SMALL to "REQUEST_TOO_SMALL",
                ErrorCode.RESPONSE_GENERATION_ERROR to "RESPONSE_GENERATION_ERROR",
                -999 to "ERROR",
            )
        for ((code, name) in names) {
            val mapped = aiError(code).toBackendException()
            assertEquals("ML Kit error $name ($code): boom", assertIs<BackendException.Failed>(mapped).message)
        }
    }
}
