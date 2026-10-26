package com.jantursky.proctor.ios

import com.jantursky.proctor.RunResult
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile

/**
 * Writes result files to [directoryPath]. The default, [inDocuments], is the app's Documents folder, which you can
 * copy out of a simulator or a device with Xcode or `xcrun simctl`. Nothing leaves the device by itself.
 */
public class ResultStore(
    public val directoryPath: String,
) {
    /** Writes [result] under its own file name and returns the file's path. */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    public fun write(result: RunResult): String {
        val created = NSFileManager.defaultManager.createDirectoryAtPath(directoryPath, true, null, null)
        check(created) { "Cannot create $directoryPath" }
        val path = "$directoryPath/${result.fileName()}"
        val written = NSString.create(string = result.toJson()).writeToFile(path, true, NSUTF8StringEncoding, null)
        check(written) { "Cannot write $path" }
        return path
    }

    public companion object {
        /** The `proctor` folder inside the app's Documents directory. */
        public fun inDocuments(): ResultStore {
            val documents =
                NSSearchPathForDirectoriesInDomains(
                    NSDocumentDirectory,
                    NSUserDomainMask,
                    true,
                ).first() as String
            return ResultStore("$documents/proctor")
        }
    }
}
