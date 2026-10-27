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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.jantursky.proctor.sample.SampleCaseDef
import com.jantursky.proctor.sample.SampleGroup
import com.jantursky.proctor.sample.SampleRow
import com.jantursky.proctor.sample.groupHistory

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
    var confirmClear by remember { mutableStateOf(false) }
    val groups = groupHistory(model.history)
    Page("History", onBack = model::back) {
        if (groups.isEmpty()) {
            Text(
                "No saved runs yet. Every run is saved as a result file on this device and listed here.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.body,
            )
        } else {
            groups.forEach { group -> GroupCard(group) { model.open(Screen.Detail(it)) } }
            OutlinedButton(
                onClick = { confirmClear = true },
                enabled = !model.running,
                border = BorderStroke(1.dp, colors.fail),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Clear history", color = colors.fail) }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = {
                Text(
                    "This deletes all ${model.history.size} saved result files on this device. " +
                        "It cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    model.clearHistory()
                    confirmClear = false
                }) { Text("Delete all", color = colors.fail) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = colors.ink) } },
        )
    }
}

@Composable
private fun GroupCard(
    group: SampleGroup,
    onOpen: (SampleRow) -> Unit,
) {
    val colors = proctorColors
    val title = if (group.id == null) "Ungrouped runs" else formatStarted(group.startedAt)
    Card(title) {
        Text(
            "${group.rows.size} ${if (group.rows.size == 1) "run" else "runs"} · ${group.backend}" +
                (group.modelVersion?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = ProctorMono,
            color = colors.caption,
        )
        Column {
            group.rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = colors.hairline)
                HistoryRow(row) { onOpen(row) }
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
            formatStarted(row.startedAt),
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
    model: SampleViewModel,
) {
    val colors = proctorColors
    Page(row.suite, model::back) {
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
        Card("Cases") { CaseList(row.cases) { model.open(Screen.Edit(row, it)) } }
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
private fun CaseList(
    cases: List<SampleCase>,
    onEdit: (SampleCase) -> Unit,
) {
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
        shown.forEach { CaseItem(it) { onEdit(it) } }
    }
}

@Composable
private fun CaseItem(
    case: SampleCase,
    onEdit: (() -> Unit)?,
) {
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
        if (onEdit != null) {
            Text(
                "Edit and run",
                style = MaterialTheme.typography.labelLarge,
                color = colors.link,
                modifier = Modifier.clickable(onClick = onEdit).padding(top = 4.dp),
            )
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

/** Change the input and the expected answer of a case and run it again on the model chosen on the home screen. */
@Composable
fun EditScreen(
    row: SampleRow,
    case: SampleCase,
    model: SampleViewModel,
) {
    val colors = proctorColors
    var input by remember { mutableStateOf(case.input) }
    var expected by remember { mutableStateOf(case.expected.orEmpty()) }
    Page("Edit case", model::back) {
        Card("${row.suite} · prompt ${row.promptVersion}") {
            Text(
                "Runs on the model selected on the home screen, with the same prompt version and graders. " +
                    "Nothing is saved.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.body,
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Input") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = expected,
                onValueChange = { expected = it },
                label = { Text("Expected output") },
                supportingText = { Text("Leave empty to run without an expected value.") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { model.runCustom(row, input, expected) },
                enabled = model.canRun && !model.customRunning && input.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.ink,
                        contentColor = colors.onInk,
                        disabledContainerColor = colors.band,
                        disabledContentColor = colors.caption,
                    ),
                shape = RoundedCornerShape(8.dp),
            ) { Text(if (model.customRunning) "Running..." else "Run this case") }
            if (!model.canRun) {
                Text(
                    "The model is not available. Check it on the home screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.notGraded,
                )
            }
        }
        model.customError?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.fail) }
        model.customResult?.let { result -> Card("Result") { CaseItem(result, onEdit = null) } }
    }
}

/** The cases the next run uses. Tap one to change its input or its expected value. */
@Composable
fun DatasetScreen(model: SampleViewModel) {
    val colors = proctorColors
    Page("Cases", onBack = model::back) {
        Text(
            "Change the input or the expected output of any case. The next run uses your changes, " +
                "until the app restarts. ${model.editedCount} of ${model.cases.size} edited.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.body,
        )
        model.cases.groupBy { it.suite }.forEach { (suite, cases) ->
            Card(suite) {
                Column {
                    cases.forEachIndexed { index, case ->
                        if (index > 0) HorizontalDivider(color = colors.hairline)
                        DatasetRow(case) { model.open(Screen.DatasetCase(case)) }
                    }
                }
            }
        }
        OutlinedButton(
            onClick = model::resetAllCases,
            enabled = model.editedCount > 0,
            border = BorderStroke(1.dp, colors.hairline),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Reset all to defaults", color = colors.ink) }
    }
}

@Composable
private fun DatasetRow(
    case: SampleCaseDef,
    onOpen: () -> Unit,
) {
    val colors = proctorColors
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                case.id,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = ProctorMono,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            if (case.edited) Text("EDITED", style = MaterialTheme.typography.labelSmall, color = colors.link)
        }
        Text(case.input, style = MaterialTheme.typography.bodyMedium, color = colors.body, maxLines = 2)
        Text(
            "expected: ${case.expected ?: "none"}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = ProctorMono,
            color = colors.caption,
            maxLines = 1,
        )
    }
}

@Composable
fun DatasetCaseScreen(
    case: SampleCaseDef,
    model: SampleViewModel,
) {
    val colors = proctorColors
    var input by remember { mutableStateOf(case.input) }
    var expected by remember { mutableStateOf(case.expected.orEmpty()) }
    Page(case.id, model::back) {
        Card(case.suite) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Input") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = expected,
                onValueChange = { expected = it },
                label = { Text("Expected output") },
                supportingText = { Text("Leave empty to run without an expected value.") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    model.saveCase(case.suite, case.id, input, expected)
                    model.back()
                },
                enabled = input.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.ink,
                        contentColor = colors.onInk,
                        disabledContainerColor = colors.band,
                        disabledContentColor = colors.caption,
                    ),
                shape = RoundedCornerShape(8.dp),
            ) { Text("Save for the next run") }
            OutlinedButton(
                onClick = {
                    model.resetCase(case.suite, case.id)
                    model.back()
                },
                enabled = case.edited,
                border = BorderStroke(1.dp, colors.hairline),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Reset this case", color = colors.ink) }
        }
    }
}
