package com.jantursky.proctor.sample.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

/**
 * Runs the sample suites on Gemini Nano while this activity is the foreground app, which is what AICore requires.
 * Start it with the launcher icon: an instrumented test restarts the app process, so it cannot run beside this
 * screen. Result files land in the app's external files directory.
 */
class MainActivity : ComponentActivity() {
    private val model: SampleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent { ProctorTheme { SampleScreen(model) } }
    }

    override fun onResume() {
        super.onResume()
        model.resumed = true
    }

    override fun onPause() {
        model.resumed = false
        super.onPause()
    }
}
