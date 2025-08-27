package com.ramitsuri.notificationjournal.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextButtonDefaults
import androidx.wear.compose.material3.TimeText
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.presentation.theme.NotificationJournalTheme
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun WearApp(
    viewState: ViewState,
    onAddRequested: (String) -> Unit,
    onTemplateAddRequested: (String) -> Unit,
    onTransferRequested: () -> Unit,
) {
    NotificationJournalTheme {
        AppScaffold(
            timeText = {
                TimeText()
            },
        ) {
            val listState = rememberTransformingLazyColumnState()
            val launcher =
                rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    it.processResult(onAddRequested)
                }
            ScreenScaffold(
                scrollState = listState,
                edgeButton = {
                    EdgeButton(
                        onClick = { launcher.launchInputActivity() },
                        buttonSize = EdgeButtonSize.Medium,
                    ) {
                        Text(stringResource(R.string.add_new))
                    }
                },
            ) { contentPadding ->
                TransformingLazyColumn(
                    contentPadding = contentPadding,
                    state = listState,
                ) {
                    val showOnDeviceEntries = viewState.journalEntries.isNotEmpty()
                    if (showOnDeviceEntries) {
                        val count = viewState.journalEntries.size
                        item {
                            Text(
                                text =
                                    pluralStringResource(
                                        id = R.plurals.journal_entries_count,
                                        count = count,
                                        count,
                                    ),
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    templateItems(
                        templates = viewState.journalEntryTemplates,
                        onTemplateAddRequested = onTemplateAddRequested,
                    )
                    if (showOnDeviceEntries) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            TransferToPhoneButton(onTransferRequested)
                        }
                    }
                }
            }
        }
    }
}

private fun TransformingLazyColumnScope.templateItems(
    templates: List<JournalEntryTemplate>,
    onTemplateAddRequested: (String) -> Unit,
) {
    templates
        .map {
            TemplateButton(
                text = it.shortDisplayText,
                onClick = { onTemplateAddRequested(it.id) },
            )
        }
        .chunked(2)
        .forEach { templateButtons ->
            item {
                TemplateButtonRow(templateButtons)
            }
        }
}

@Composable
private fun TemplateButtonRow(templateButtons: List<TemplateButton>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        templateButtons.forEach {
            LargeButton(
                modifier = Modifier.weight(1f),
                onClick = it.onClick,
                text = it.text,
            )
        }
    }
}

@Composable
private fun TransferToPhoneButton(onTransferRequested: () -> Unit) {
    TextButton(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        onClick = { onTransferRequested() },
    ) {
        Text(text = stringResource(id = R.string.transfer_to_phone))
    }
}

@Composable
private fun LargeButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
) {
    TextButton(
        modifier =
            modifier
                .padding(bottom = 8.dp),
        colors = TextButtonDefaults.filledTonalTextButtonColors(),
        onClick = onClick,
    ) {
        Text(text = text, maxLines = 1, style = MaterialTheme.typography.displayMedium)
    }
}

private data class TemplateButton(val text: String, val onClick: () -> Unit)

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
private fun DefaultPreview() {
    WearApp(viewState = ViewState(), { }, { }, { })
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
private fun JournalEntriesPresentPreview() {
    WearApp(
        viewState =
            ViewState(
                journalEntries =
                    listOf(
                        JournalEntry(
                            entryTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                            text = "Text1",
                        ),
                        JournalEntry(
                            entryTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                            text = "Text2",
                        ),
                        JournalEntry(
                            entryTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                            text = "Text3",
                        ),
                    ),
            ),
        { },
        { },
        { },
    )
}
