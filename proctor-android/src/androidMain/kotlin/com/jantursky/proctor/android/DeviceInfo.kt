package com.jantursky.proctor.android

import android.os.Build

/** What the result file records about the phone. Separate from [Build] so it can be faked in host tests. */
internal data class DeviceInfo(
    val model: String,
    val osVersion: String,
) {
    companion object {
        fun current(): DeviceInfo =
            DeviceInfo(
                model = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}, build ${Build.DISPLAY})",
            )
    }
}
