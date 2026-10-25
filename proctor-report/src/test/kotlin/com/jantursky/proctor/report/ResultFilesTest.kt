package com.jantursky.proctor.report

import com.jantursky.proctor.Status.Passed
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ResultFilesTest {
    private fun tempDir() = Files.createTempDirectory("proctor-report-test")

    @Test
    fun readsResultFilesOldestFirstAndIgnoresOtherFiles() {
        val dir = tempDir()
        val newer = run(case("a", Passed), startedAt = "2026-10-26T11:00:00Z")
        val older = run(case("a", Passed), startedAt = "2026-10-26T10:00:00Z")
        dir.resolve("sub").createDirectories()
        dir.resolve(newer.fileName()).writeText(newer.toJson())
        dir.resolve("sub").resolve(older.fileName()).writeText(older.toJson())
        dir.resolve("notes.txt").writeText("ignore me")
        dir.resolve("other.json").writeText("{}")

        assertEquals(listOf(older, newer), ResultFiles.readAll(dir))
    }

    @Test
    fun namesTheBrokenFile() {
        val dir = tempDir()
        dir.resolve("bad.proctor.json").writeText("{}")

        val error = assertFailsWith<IllegalArgumentException> { ResultFiles.readAll(dir) }
        assertTrue(error.message!!.startsWith("bad.proctor.json: Not a Proctor result file"))
    }

    @Test
    fun refusesAPathThatIsNotADirectory() {
        val file = Files.createTempFile("proctor-report-test", ".txt")
        assertTrue(
            assertFailsWith<IllegalArgumentException> {
                ResultFiles.readAll(file)
            }.message!!.contains("not a directory"),
        )
    }
}
