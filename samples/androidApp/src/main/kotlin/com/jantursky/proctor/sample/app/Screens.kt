package com.jantursky.proctor.sample.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jantursky.proctor.Status
import com.jantursky.proctor.sample.SampleCase
import com.jantursky.proctor.sample.SampleRow

/** A screen with a back button and a title, used by every screen but the home screen. */
@Composable
private fun Page(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                border = BorderStroke(1.dp, colors.hairline),
                shape = RoundedCornerShape(8.dp),
            ) { Text("Back", color = colors.ink) }
            Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.ink)
        }
        content()
    }
}

@Composable
fun HistoryScreen(model: SampleViewModel) {
    val colors = proctorColors
    Page("History", onBack = model::back) {
        if (model.history.isEmpty()) {
            Text(
                "No saved runs yet. Every run is saved as a result file on this device and listed here.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.body,
            )
        } else {
            Card("${model.history.size} saved runs") {
                Column {
                    model.history.forEachIndexed { index, row ->
                        if (index > 0) HorizontalDivider(color = colors.hairline)
                        HistoryRow(row) { model.open(Screen.Detail(row)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerdictBadge(passed: Boolean) {
    val colors = proctorColors
    val tone = if (passed) colors.pass else colors.fail
    Text(
        if (passed) "PASS" else "FAIL",
        style = MaterialTheme.typography.labelSmall,
        color = tone,
        modifier =
            Modifier
                .border(BorderStroke(1.dp, tone), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun HistoryRow(
    row: SampleRow,
    onOpen: () -> Unit,
) {
    val colors = proctorColors
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${row.suite} · ${row.promptVersion}",
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            VerdictBadge(row.suitePassed)
        }
        Text(
            "${row.passed}/${row.total} passed" +
                (if (row.failed > 0) ", ${row.failed} failed" else "") +
                (if (row.notGraded > 0) ", ${row.notGraded} not graded" else ""),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.body,
        )
        Text(
            "${formatStarted(row.startedAt)} · ${row.backend}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = ProctorMono,
            color = colors.caption,
        )
    }
}

/** Turns a result file's start stamp into `2026-10-27 01:03 UTC`. */
private fun formatStarted(startedAt: String): String {
    val date = startedAt.substringBefore('T')
    val time = startedAt.substringAfter('T').take(5).replace('-', ':')
    return "$date $time UTC"
}

@Composable
fun DetailScreen(
    row: SampleRow,
    onBack: () -> Unit,
) {
    val colors = proctorColors
    Page(row.suite, onBack) {
        Card("Run") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "prompt ${row.promptVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = ProctorMono,
                    color = colors.body,
                    modifier = Modifier.weight(1f),
                )
                VerdictBadge(row.suitePassed)
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
            HorizontalDivider(color = colors.hairline)
            Detail("Started", formatStarted(row.startedAt))
            Detail("Device", "${row.deviceModel}, ${row.osVersion}")
            Detail("Model version", row.modelVersion ?: "not reported")
            Detail("Backend", row.backend)
            row.availabilityNote?.let { Detail("Availability", it) }
        }
        Card("Cases") { CaseList(row.cases) }
    }
}

private enum class CaseFilter(
    val label: String,
) {
    All("All"),
    Failed("Failed"),
    Passed("Passed"),
}

@Composable
private fun CaseList(cases: List<SampleCase>) {
    val colors = proctorColors
    var filter by remember {
        mutableStateOf(if (cases.any { it.status != Status.Passed }) CaseFilter.Failed else CaseFilter.All)
    }
    val shown =
        cases.filter {
            when (filter) {
                CaseFilter.All -> true
                CaseFilter.Failed -> it.status != Status.Passed
                CaseFilter.Passed -> it.status == Status.Passed
            }
        }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            CaseFilter.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == filter,
                    onClick = { filter = option },
                    shape = SegmentedButtonDefaults.itemShape(index, CaseFilter.entries.size),
                    colors =
                        SegmentedButtonDefaults.colors(
                            activeContainerColor = colors.ink,
                            activeContentColor = colors.onInk,
                            activeBorderColor = colors.ink,
                            inactiveBorderColor = colors.hairline,
                            inactiveContentColor = colors.ink,
                        ),
                    icon = {},
                ) { Text(option.label, style = MaterialTheme.typography.labelLarge) }
            }
        }
        if (shown.isEmpty()) Text("No cases here.", style = MaterialTheme.typography.bodyMedium, color = colors.body)
        shown.forEach { CaseItem(it) }
    }
}

@Composable
private fun CaseItem(case: SampleCase) {
    val colors = proctorColors
    val tone =
        when (case.status) {
            Status.Passed -> colors.pass
            Status.Failed -> colors.fail
            Status.NotGraded -> colors.notGraded
        }
    val label =
        when (case.status) {
            Status.Passed -> "PASS"
            Status.Failed -> "FAIL"
            Status.NotGraded -> "NOT GRADED"
        }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.band)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                case.id,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = ProctorMono,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = tone)
        }
        Text(case.input, style = MaterialTheme.typography.bodyMedium, color = colors.body)
        case.expected?.let { Detail("Expected", it) }
        case.attempts.forEachIndexed { index, attempt ->
            val prefix = if (case.attempts.size > 1) "Attempt ${index + 1}" else "Output"
            Detail(prefix, attempt.output ?: "(no output)")
            attempt.reason?.let { Detail("Reason", it) }
        }
    }
}

@Composable
internal fun Detail(
    label: String,
    value: String,
) {
    val colors = proctorColors
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.caption)
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = ProctorMono, color = colors.ink)
    }
}
