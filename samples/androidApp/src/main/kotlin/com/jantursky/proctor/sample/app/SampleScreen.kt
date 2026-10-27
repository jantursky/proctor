package com.jantursky.proctor.sample.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jantursky.proctor.Status
import com.jantursky.proctor.android.NanoPreference
import com.jantursky.proctor.android.NanoRelease
import com.jantursky.proctor.sample.SampleCase
import com.jantursky.proctor.sample.SampleRow
import kotlin.math.roundToInt

@Composable
fun SampleScreen(model: SampleViewModel) {
    BackHandler(enabled = model.canGoBack) { model.back() }
    when (val screen = model.screen) {
        Screen.Home -> HomeScreen(model)
        Screen.History -> HistoryScreen(model)
        is Screen.Detail -> DetailScreen(screen.row, model::back)
    }
}

@Composable
private fun HomeScreen(model: SampleViewModel) {
    val colors = proctorColors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Header(onHistory = { model.open(Screen.History) })
        ModelCard(model)
        RunCard(model)
        if (model.rows.isNotEmpty()) ResultsCard(model)
        Text(
            "Prompts and outputs stay on this device. Nothing is sent anywhere.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.caption,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun Header(onHistory: () -> Unit) {
    val colors = proctorColors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Image(painterResource(R.drawable.ic_mark), contentDescription = null, modifier = Modifier.size(48.dp))
        Column(Modifier.weight(1f)) {
            Text("Proctor", style = MaterialTheme.typography.headlineMedium, color = colors.ink)
            Text(
                "Evaluation harness for on-device LLMs",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.body,
            )
        }
        OutlinedButton(
            onClick = onHistory,
            border = BorderStroke(1.dp, colors.hairline),
            shape = RoundedCornerShape(8.dp),
        ) { Text("History", color = colors.ink) }
    }
}

@Composable
internal fun Card(
    title: String,
    content: @Composable () -> Unit,
) {
    val colors = proctorColors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, colors.hairline), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.caption)
        content()
    }
}

@Composable
private fun ModelCard(model: SampleViewModel) {
    val colors = proctorColors
    Card("Model") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Choice(
                "Release",
                NanoRelease.entries.map { it.name },
                model.config.release.name,
                !model.running && !model.downloading,
            ) {
                model.select(release = NanoRelease.valueOf(it))
            }
            Choice(
                "Variant",
                NanoPreference.entries.map { it.name },
                model.config.preference.name,
                !model.running && !model.downloading,
            ) { model.select(preference = NanoPreference.valueOf(it)) }
            Text(
                model.config.describe(),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = ProctorMono,
                color = colors.caption,
            )
        }
        HorizontalDivider(color = colors.hairline)
        StatusRow(model.status)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (model.canDownload || model.downloading) {
                Button(
                    onClick = { if (model.downloading) model.cancelDownload() else model.download() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.ink, contentColor = colors.onInk),
                    shape = RoundedCornerShape(8.dp),
                ) { Text(if (model.downloading) "Cancel download" else "Download model") }
            }
            OutlinedButton(
                onClick = model::check,
                enabled = !model.running && !model.downloading,
                border = BorderStroke(1.dp, colors.hairline),
                shape = RoundedCornerShape(8.dp),
            ) { Text("Check again", color = colors.ink) }
        }
    }
}

@Composable
private fun Choice(
    label: String,
    options: List<String>,
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    val colors = proctorColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.body, modifier = Modifier.width(72.dp))
        SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    colors =
                        SegmentedButtonDefaults.colors(
                            activeContainerColor = colors.ink,
                            activeContentColor = colors.onInk,
                            activeBorderColor = colors.ink,
                            inactiveBorderColor = colors.hairline,
                            inactiveContentColor = colors.ink,
                        ),
                    icon = {},
                ) { Text(option, style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

@Composable
private fun StatusRow(status: ModelStatus) {
    val colors = proctorColors
    val dot =
        when (status.tone) {
            Tone.Good -> colors.pass
            Tone.Warn -> colors.notGraded
            Tone.Bad -> colors.fail
            Tone.Neutral -> colors.caption
        }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.padding(top = 6.dp)) {
            if (status.checking) {
                CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp, color = colors.caption)
            } else {
                Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(status.title, style = MaterialTheme.typography.titleMedium, color = colors.ink)
            status.detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.body) }
        }
    }
}

@Composable
private fun RunCard(model: SampleViewModel) {
    val colors = proctorColors
    Card("Evaluation") {
        Text(
            "Runs ticket-classifier (prompt v1 and v2) and ticket-extraction, three attempts per case. " +
                "Keep this screen open: AICore only serves the app in the foreground.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.body,
        )
        Button(
            onClick = model::run,
            enabled = model.canRun && !model.running,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colors.ink,
                    contentColor = colors.onInk,
                    disabledContainerColor = colors.band,
                    disabledContentColor = colors.caption,
                ),
            shape = RoundedCornerShape(8.dp),
        ) { Text(if (model.running) "Running..." else "Run sample suites") }
        if (model.running && model.total > 0) {
            val fraction = model.done.toFloat() / model.total
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
                color = colors.ink,
                trackColor = colors.hairline,
            )
            Text(
                progressText(model),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = ProctorMono,
                color = colors.caption,
            )
            if (model.waiting) {
                Text(
                    "AICore asked the app to slow down. Waiting before the next try.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.notGraded,
                )
            }
        }
        model.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.ink) }
    }
}

@Composable
private fun ResultsCard(model: SampleViewModel) {
    val colors = proctorColors
    Card("Results") {
        Column {
            model.rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = colors.hairline)
                ResultRow(row) { model.open(Screen.Detail(row)) }
            }
        }
        model.resultsDirectory?.let {
            Text(
                "Files: $it",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = ProctorMono,
                color = colors.caption,
            )
        }
    }
}

@Composable
private fun progressText(model: SampleViewModel): String {
    val percent = (model.done * 100f / model.total).roundToInt()
    val left =
        model.remainingSeconds
            ?.let { seconds ->
                if (seconds >= 90) ", about ${(seconds / 60f).roundToInt()} min left" else ", about $seconds s left"
            }.orEmpty()
    return "${model.done} of ${model.total} requests, $percent%$left"
}

@Composable
private fun ResultRow(
    row: SampleRow,
    onOpen: () -> Unit,
) {
    val colors = proctorColors
    val verdict = if (row.suitePassed) colors.pass else colors.fail
    Column(
        Modifier.clickable(onClick = onOpen).padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.suite, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                Text(
                    "prompt ${row.promptVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = ProctorMono,
                    color = colors.caption,
                )
            }
            Text(
                if (row.suitePassed) "PASS" else "FAIL",
                style = MaterialTheme.typography.labelSmall,
                color = verdict,
                modifier =
                    Modifier
                        .border(BorderStroke(1.dp, verdict), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
            Text(
                "${row.passed}/${row.total}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
            )
            Text("passed", style = MaterialTheme.typography.bodyMedium, color = colors.body)
            if (row.failed >
                0
            ) {
                Text("${row.failed} failed", style = MaterialTheme.typography.bodyMedium, color = colors.fail)
            }
            if (row.notGraded > 0) {
                Text(
                    "${row.notGraded} not graded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.notGraded,
                )
            }
        }
        row.meanScore?.let { ScoreBar(it) }
        Text("View cases", style = MaterialTheme.typography.labelLarge, color = colors.link)
    }
}

@Composable
internal fun ScoreBar(score: Double) {
    val colors = proctorColors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.hairline),
        ) {
            Box(Modifier.fillMaxWidth(score.toFloat().coerceIn(0f, 1f)).height(6.dp).background(colors.ink))
        }
        Text(
            "${(score * 100).roundToInt()}%",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = ProctorMono,
            color = colors.body,
        )
    }
}
