package com.jantursky.proctor.android

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

/**
 * An empty activity that keeps the app in the foreground and the screen on while a suite runs. Launch it before the
 * run (`ActivityScenario.launch(ProctorHostActivity::class.java)` in an instrumented test) and wrap the backend in
 * [ForegroundBackend] with [isForeground].
 */
public class ProctorHostActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(
            TextView(this).apply {
                text = "Proctor run in progress.\nKeep this screen in the foreground."
                gravity = Gravity.CENTER
                textSize = 18f
            },
        )
    }

    override fun onResume() {
        super.onResume()
        resumed = true
    }

    override fun onPause() {
        resumed = false
        super.onPause()
    }

    public companion object {
        @Volatile
        private var resumed = false

        /** `true` while a [ProctorHostActivity] is resumed, that is, the top foreground activity. */
        public fun isForeground(): Boolean = resumed
    }
}
