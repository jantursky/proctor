package com.jantursky.proctor.ios

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice
import platform.posix.uname
import platform.posix.utsname

internal data class DeviceInfo(
    val model: String,
    val osVersion: String,
    val systemVersion: String,
    val simulator: Boolean,
    /** The Darwin release the process sees. In a simulator that is the Mac host's, where the model actually runs. */
    val kernelRelease: String?,
) {
    companion object {
        fun current(): DeviceInfo {
            val simulatorModel = NSProcessInfo.processInfo.environment["SIMULATOR_MODEL_IDENTIFIER"] as? String
            return DeviceInfo(
                // On a device `uname` reports the hardware identifier, such as iPhone17,1. On a simulator it
                // reports the Mac's architecture, so the simulated model comes from the environment.
                model = simulatorModel ?: hardwareMachine() ?: UIDevice.currentDevice.model,
                osVersion = NSProcessInfo.processInfo.operatingSystemVersionString,
                systemVersion = UIDevice.currentDevice.systemVersion,
                simulator = simulatorModel != null,
                kernelRelease = kernelRelease(),
            )
        }

        @OptIn(ExperimentalForeignApi::class)
        private fun hardwareMachine(): String? =
            memScoped {
                val info = alloc<utsname>()
                if (uname(info.ptr) != 0) null else info.machine.toKString()
            }

        @OptIn(ExperimentalForeignApi::class)
        private fun kernelRelease(): String? =
            memScoped {
                val info = alloc<utsname>()
                if (uname(info.ptr) != 0) null else info.release.toKString()
            }
    }
}
