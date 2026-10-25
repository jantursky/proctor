package com.jantursky.proctor.report

import com.jantursky.proctor.RunResult
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.walk

/** Reads the JSON files that runners write on the device. */
public object ResultFiles {
    private const val SUFFIX = ".proctor.json"

    /** Reads one result file. @throws IllegalArgumentException if it is not a Proctor result. */
    public fun read(file: Path): RunResult =
        try {
            RunResult.fromJson(file.readText())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("${file.name}: ${e.message}", e)
        }

    /** Reads every `*.proctor.json` under [directory], oldest run first. Other files are ignored. */
    public fun readAll(directory: Path): List<RunResult> {
        require(directory.isDirectory()) { "$directory is not a directory" }
        return directory
            .walk()
            .filter { it.name.endsWith(SUFFIX) && it.extension == "json" }
            .map(::read)
            .sortedBy { it.startedAt }
            .toList()
    }
}
