package com.jantursky.proctor.android

import android.content.Context
import com.jantursky.proctor.RunResult
import java.io.File

/**
 * Writes result files where `adb pull` can reach them without extra permissions: the app's external files directory,
 * `/sdcard/Android/data/<package>/files/proctor/`. Nothing leaves the device by itself.
 */
public class ResultStore(
    public val directory: File,
) {
    /** Writes [result] under its own file name and returns the file. */
    public fun write(result: RunResult): File {
        directory.mkdirs()
        return File(directory, result.fileName()).also { it.writeText(result.toJson()) }
    }

    public companion object {
        /** The store in the app's external files directory, falling back to its internal one. */
        public fun forApp(context: Context): ResultStore {
            val base = context.getExternalFilesDir(null) ?: context.filesDir
            return ResultStore(File(base, "proctor"))
        }
    }
}
