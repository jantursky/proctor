package com.jantursky.proctor.ios

import com.jantursky.proctor.EvalRunner
import com.jantursky.proctor.FakeBackend
import com.jantursky.proctor.RunResult
import com.jantursky.proctor.RunnerConfig
import com.jantursky.proctor.evalSuite
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUUID
import platform.Foundation.stringWithContentsOfFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ResultStoreTest {
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    @Test
    fun writesTheResultUnderItsFileNameAndItReadsBack() =
        runTest {
            val suite =
                evalSuite("s") {
                    prompt("v1") { it }
                    case("a") { expect("A") }
                    graders { exactMatch() }
                }
            val result = EvalRunner(FakeBackend { _, _ -> "A" }, RunnerConfig(requestDelay = 0.seconds)).run(suite)
            val directory = NSTemporaryDirectory() + "proctor-" + NSUUID().UUIDString + "/nested"

            val path = ResultStore(directory).write(result)

            assertEquals("$directory/${result.fileName()}", path)
            val text = NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) as String
            assertEquals(result, RunResult.fromJson(text))
            NSFileManager.defaultManager.removeItemAtPath(directory, null)
        }

    @Test
    fun theDefaultStoreLivesInDocuments() {
        assertTrue(ResultStore.inDocuments().directoryPath.endsWith("/Documents/proctor"))
    }
}
