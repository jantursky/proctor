package com.jantursky.proctor.sample.app

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.jantursky.proctor.EvalRunner
import com.jantursky.proctor.RunAbortedException
import com.jantursky.proctor.android.ForegroundBackend
import com.jantursky.proctor.android.MlKitBackend
import com.jantursky.proctor.android.MlKitConfig
import com.jantursky.proctor.android.ResultStore
import com.jantursky.proctor.sample.sampleSuites
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Runs the sample suites on the stable Gemini Nano model and shows one line per run. This activity is the foreground
 * app, so AICore serves it; the result files land in the app's external files directory.
 */
class MainActivity : Activity() {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    @Volatile
    private var resumed = false
    private lateinit var output: TextView
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        output = TextView(this).apply { text = "Keep this screen open while the suites run." }
        button = Button(this).apply { text = "Run sample suites" }
        button.setOnClickListener { run() }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 96, 48, 48)
                addView(button)
                addView(ScrollView(context).apply { addView(output) })
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

    override fun onDestroy() {
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    private fun run() {
        if (job?.isActive == true) return
        button.isEnabled = false
        output.text = ""
        val store = ResultStore.forApp(this)
        job =
            scope.launch {
                val backend = ForegroundBackend(MlKitBackend(MlKitConfig()), ::resumedNow)
                try {
                    for (suite in sampleSuites) {
                        for (result in EvalRunner(backend).runAll(suite)) {
                            store.write(result)
                            val s = result.summary
                            append(
                                "${suite.name} ${result.promptVersion}: ${s.passed}/${s.total} passed, ${s.notGraded} not graded",
                            )
                        }
                    }
                    append("Done. Files in ${store.directory}")
                } catch (e: RunAbortedException) {
                    append("Run aborted: ${e.message}")
                } finally {
                    runOnUiThread { button.isEnabled = true }
                }
            }
    }

    private fun resumedNow() = resumed

    private fun append(line: String) = runOnUiThread { output.append(line + "\n") }
}
