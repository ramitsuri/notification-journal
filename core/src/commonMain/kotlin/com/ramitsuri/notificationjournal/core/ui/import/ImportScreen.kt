package com.ramitsuri.notificationjournal.core.ui.import

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ramitsuri.notificationjournal.core.ui.components.Date
import com.ramitsuri.notificationjournal.core.ui.components.Toolbar
import com.ramitsuri.notificationjournal.core.utils.getDay
import kotlinx.datetime.LocalDate
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.import
import notificationjournal.core.generated.resources.import_end_date
import notificationjournal.core.generated.resources.import_from_dir
import notificationjournal.core.generated.resources.import_from_dir_hint
import notificationjournal.core.generated.resources.import_start_date
import notificationjournal.core.generated.resources.import_status_completed
import notificationjournal.core.generated.resources.import_status_in_progress
import notificationjournal.core.generated.resources.ok
import notificationjournal.core.generated.resources.pick
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    state: ViewState,
    onBackClick: () -> Unit,
    onImportClick: () -> Unit,
    onFromDirChanged: (String) -> Unit,
    onStartDateChanged: (LocalDate) -> Unit,
    onResetStartDate: () -> Unit,
    onEndDateChanged: (LocalDate) -> Unit,
    onResetEndDate: () -> Unit,
) {
    var showImportStatus by remember { mutableStateOf(false) }

    LaunchedEffect(state.importStatus) {
        if (state.importStatus is ImportStatus.Completed ||
            state.importStatus is ImportStatus.InProgress
        ) {
            showImportStatus = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal,
                    ),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Toolbar(
                onBackClick = onBackClick,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OutlinedTextField(
                    value = state.fromDir,
                    singleLine = true,
                    label = {
                        Text(stringResource(Res.string.import_from_dir))
                    },
                    placeholder = {
                        Text(stringResource(Res.string.import_from_dir_hint))
                    },
                    onValueChange = onFromDirChanged,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                DatePickers(
                    startDate = state.startDate,
                    allowedStartDateSelections = state.allowedStartDateSelections,
                    endDate = state.endDate,
                    allowedEndDateSelections = state.allowedEndDateSelections,
                    onStartDateChanged = onStartDateChanged,
                    onResetStartDate = onResetStartDate,
                    onEndDateChanged = onEndDateChanged,
                    onResetEndDate = onResetEndDate,
                )
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = onImportClick,
                    enabled = state.isImportEnabled
                ) {
                    Text(stringResource(Res.string.import))
                }
            }

            if (showImportStatus) {
                ImportStatus(
                    status = state.importStatus,
                    onCompletedAcknowledged = {
                        showImportStatus = false
                        onBackClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun ImportStatus(
    status: ImportStatus,
    onCompletedAcknowledged: () -> Unit
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Card {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (status) {
                    is ImportStatus.InProgress -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(
                                Res.string.import_status_in_progress,
                                status.importCount,
                            )
                        )
                    }

                    is ImportStatus.Completed -> {
                        Text(
                            text = stringResource(
                                Res.string.import_status_completed,
                                status.importCount,
                                status.daysCount,
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = onCompletedAcknowledged) {
                                Text(stringResource(Res.string.ok))
                            }
                        }
                    }

                    is ImportStatus.NotStarted -> {
                        // Shouldn't happen
                    }
                }
            }
        }
    }
}

@Composable
private fun DatePickers(
    startDate: LocalDate?,
    allowedStartDateSelections: ClosedRange<LocalDate>,
    endDate: LocalDate?,
    allowedEndDateSelections: ClosedRange<LocalDate>,
    onStartDateChanged: (LocalDate) -> Unit,
    onResetStartDate: () -> Unit,
    onEndDateChanged: (LocalDate) -> Unit,
    onResetEndDate: () -> Unit,
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.import_start_date),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.width(16.dp))
            TextButton(
                onClick = { showStartDatePicker = true },
                modifier = Modifier.weight(1f),
            ) {
                val text =
                    startDate?.let { getDay(it) } ?: stringResource(Res.string.pick)
                Text(text)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.import_end_date),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.width(16.dp))
            TextButton(
                onClick = { showEndDatePicker = true },
                modifier = Modifier.weight(1f),
            ) {
                val text = endDate?.let { getDay(it) } ?: stringResource(Res.string.pick)
                Text(text)
            }
        }
    }
    if (showStartDatePicker) {
        Date(
            selectedDate = startDate,
            allowedSelections = allowedStartDateSelections,
            onDateSelected = {
                showStartDatePicker = false
                onStartDateChanged(it)
            },
            onResetDateToToday = null,
            onResetDate = onResetStartDate,
            onDismiss = { showStartDatePicker = false },
        )
    }
    if (showEndDatePicker) {
        Date(
            selectedDate = endDate,
            allowedSelections = allowedEndDateSelections,
            onDateSelected = {
                showEndDatePicker = false
                onEndDateChanged(it)
            },
            onResetDateToToday = null,
            onResetDate = onResetEndDate,
            onDismiss = { showEndDatePicker = false },
        )
    }
}
