package com.ramitsuri.notificationjournal.core.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.model.stats.EntryStats
import com.ramitsuri.notificationjournal.core.ui.components.Toolbar
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.cancel
import notificationjournal.core.generated.resources.data_host
import notificationjournal.core.generated.resources.device_name
import notificationjournal.core.generated.resources.exchange_name
import notificationjournal.core.generated.resources.more
import notificationjournal.core.generated.resources.ok
import notificationjournal.core.generated.resources.settings_app_version
import notificationjournal.core.generated.resources.settings_copy_with_empty_tags
import notificationjournal.core.generated.resources.settings_data_sharing_not_set
import notificationjournal.core.generated.resources.settings_data_sharing_title
import notificationjournal.core.generated.resources.settings_delete_all_subtitle
import notificationjournal.core.generated.resources.settings_delete_all_title
import notificationjournal.core.generated.resources.settings_journal_import_export_subtitle
import notificationjournal.core.generated.resources.settings_journal_import_export_title
import notificationjournal.core.generated.resources.settings_logs
import notificationjournal.core.generated.resources.settings_showConflictDiffInline
import notificationjournal.core.generated.resources.settings_show_empty_tags
import notificationjournal.core.generated.resources.settings_show_stats_title
import notificationjournal.core.generated.resources.settings_tags_subtitle
import notificationjournal.core.generated.resources.settings_tags_title
import notificationjournal.core.generated.resources.settings_templates_subtitle
import notificationjournal.core.generated.resources.settings_templates_title
import notificationjournal.core.generated.resources.settings_upload_subtitle
import notificationjournal.core.generated.resources.settings_upload_title
import notificationjournal.core.generated.resources.stats_column_days
import notificationjournal.core.generated.resources.stats_column_entries
import notificationjournal.core.generated.resources.stats_row_all
import notificationjournal.core.generated.resources.stats_row_not_uploaded_not_reconciled
import notificationjournal.core.generated.resources.stats_row_not_uploaded_reconciled
import notificationjournal.core.generated.resources.stats_row_uploaded_not_reconciled
import notificationjournal.core.generated.resources.stats_row_uploaded_reconciled
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    state: SettingsViewState,
    onBack: () -> Unit,
    onUploadClicked: () -> Unit,
    onDataSharingPropertiesSet: (DataHost, ExchangeName, DeviceName) -> Unit,
    onTagsClicked: () -> Unit,
    onTemplatesClicked: () -> Unit,
    onToggleShowConflictDiffInline: () -> Unit,
    onToggleShowEmptyTags: () -> Unit,
    onToggleCopyWithEmptyTags: () -> Unit,
    onLogsClicked: () -> Unit,
    onImportExportClicked: () -> Unit,
    onDeleteAll: () -> Unit,
    onShowStatsToggled: () -> Unit,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    if (showDialog) {
        DataSharingPropertiesDialog(
            dataHostProperties = state.dataHostProperties,
            onPositiveClick = { dataHost, exchangeName, deviceName ->
                showDialog = !showDialog
                onDataSharingPropertiesSet(dataHost, exchangeName, deviceName)
            },
            onNegativeClick = { showDialog = !showDialog },
        )
    }
    if (state.stats != null) {
        StatsDialog(
            stats = state.stats,
            onStatsRequestToggled = onShowStatsToggled,
        )
    }
    Surface {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .displayCutoutPadding(),
        ) {
            Toolbar(onBackClick = onBack)
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth(),
            ) {
                item {
                    val subtitle =
                        buildString {
                            if (state.dataHostProperties.dataHost.isNotEmpty()) {
                                append(state.dataHostProperties.dataHost)
                                append(" : ")
                            }
                            if (state.dataHostProperties.exchangeName.isNotEmpty()) {
                                append(state.dataHostProperties.exchangeName)
                                append(" : ")
                            }
                            if (state.dataHostProperties.deviceName.isNotEmpty()) {
                                append(state.dataHostProperties.deviceName)
                            }
                        }
                    SettingsItem(
                        title = stringResource(Res.string.settings_data_sharing_title),
                        subtitle = subtitle.ifEmpty { stringResource(Res.string.settings_data_sharing_not_set) },
                        onClick = {
                            showDialog = true
                        },
                        showProgress = false,
                    )
                }
                item {
                    SettingsItem(
                        title = stringResource(Res.string.settings_tags_title),
                        subtitle = stringResource(Res.string.settings_tags_subtitle),
                        onClick = onTagsClicked,
                        showProgress = false,
                        modifier = modifier,
                    )
                }
                item {
                    SettingsItem(
                        title = stringResource(Res.string.settings_templates_title),
                        subtitle = stringResource(Res.string.settings_templates_subtitle),
                        onClick = onTemplatesClicked,
                        showProgress = false,
                    )
                }
                item {
                    SettingsItemWithToggle(
                        title = stringResource(Res.string.settings_showConflictDiffInline),
                        value = state.showConflictDiffInline,
                        onClick = onToggleShowConflictDiffInline,
                    )
                }
                item {
                    SettingsItemWithToggle(
                        title = stringResource(Res.string.settings_show_empty_tags),
                        value = state.showEmptyTags,
                        onClick = onToggleShowEmptyTags,
                    )
                }
                item {
                    SettingsItemWithToggle(
                        title = stringResource(Res.string.settings_copy_with_empty_tags),
                        value = state.copyWithEmptyTags,
                        onClick = onToggleCopyWithEmptyTags,
                    )
                }
                item {
                    SettingsItem(
                        title = stringResource(Res.string.settings_show_stats_title),
                        onClick = onShowStatsToggled,
                        showProgress = false,
                    )
                }
                item {
                    SettingsItem(
                        title = stringResource(Res.string.settings_logs),
                        onClick = onLogsClicked,
                        showProgress = false,
                    )
                }
                item {
                    SettingsItem(
                        title = stringResource(Res.string.settings_upload_title),
                        subtitle = stringResource(Res.string.settings_upload_subtitle),
                        onClick = onUploadClicked,
                        showProgress = state.uploadLoading,
                    )
                }
                if (state.showJournalImportButton) {
                    item {
                        SettingsItem(
                            title = stringResource(Res.string.settings_journal_import_export_title),
                            subtitle = stringResource(Res.string.settings_journal_import_export_subtitle),
                            onClick = onImportExportClicked,
                            showProgress = false,
                        )
                    }
                }
                if (state.allowDelete) {
                    item {
                        SettingsItem(
                            title = stringResource(Res.string.settings_delete_all_title),
                            subtitle = stringResource(Res.string.settings_delete_all_subtitle),
                            onClick = onDeleteAll,
                            showProgress = false,
                        )
                    }
                }
                item {
                    SettingsItem(
                        title = stringResource(Res.string.settings_app_version),
                        subtitle = state.appVersion,
                        onClick = { },
                        showProgress = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String = "",
    onClick: () -> Unit,
    showProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp)
                .clickable(onClick = onClick, enabled = !showProgress)
                .padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(4.dp),
        )
        if (showProgress) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun SettingsItemWithToggle(
    title: String,
    value: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp)
                .clickable(onClick = onClick)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(4.dp),
        )
        Switch(
            checked = value,
            onCheckedChange = null,
        )
    }
}

@Composable
private fun DataSharingPropertiesDialog(
    dataHostProperties: DataHostProperties,
    onPositiveClick: (DataHost, ExchangeName, DeviceName) -> Unit,
    onNegativeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dataHostText by rememberSaveable { mutableStateOf(dataHostProperties.dataHost) }
    var exchangeNameText by rememberSaveable { mutableStateOf(dataHostProperties.exchangeName) }
    var deviceNameText by rememberSaveable { mutableStateOf(dataHostProperties.deviceName) }

    Dialog(onDismissRequest = { }) {
        Card {
            Column(modifier = modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = dataHostText,
                        singleLine = true,
                        label = {
                            Text(stringResource(Res.string.data_host))
                        },
                        onValueChange = { dataHostText = it },
                        modifier = modifier.weight(1f),
                    )
                    OtherHostsButton(
                        otherHosts = dataHostProperties.otherHosts,
                        otherHostPicked = {
                            dataHostText = it
                        },
                    )
                }
                Spacer(modifier = modifier.height(16.dp))
                OutlinedTextField(
                    value = exchangeNameText,
                    singleLine = true,
                    label = {
                        Text(stringResource(Res.string.exchange_name))
                    },
                    onValueChange = { exchangeNameText = it },
                    modifier = modifier.fillMaxWidth(),
                )
                Spacer(modifier = modifier.height(16.dp))
                OutlinedTextField(
                    value = deviceNameText,
                    singleLine = true,
                    label = {
                        Text(stringResource(Res.string.device_name))
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                    onValueChange = { deviceNameText = it },
                    modifier = modifier.fillMaxWidth(),
                )
                Spacer(modifier = modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        onClick = {
                            onNegativeClick()
                        },
                    ) {
                        Text(text = stringResource(Res.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onPositiveClick(
                                DataHost(dataHostText),
                                ExchangeName(exchangeNameText),
                                DeviceName(deviceNameText),
                            )
                        },
                    ) {
                        Text(text = stringResource(Res.string.ok))
                    }
                }
            }
        }
    }
}

@Composable
fun OtherHostsButton(
    otherHosts: Set<String>,
    otherHostPicked: (String) -> Unit,
) {
    var showOtherHosts by rememberSaveable { mutableStateOf(false) }
    if (otherHosts.isNotEmpty()) {
        Box {
            IconButton(
                onClick = { showOtherHosts = true },
                modifier =
                    Modifier
                        .size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(Res.string.more),
                )
            }
            DropdownMenu(
                expanded = showOtherHosts,
                onDismissRequest = {
                    showOtherHosts = false
                },
            ) {
                otherHosts.forEach { otherHost ->
                    DropdownMenuItem(
                        text = { Text(otherHost) },
                        onClick = {
                            showOtherHosts = false
                            otherHostPicked(otherHost)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsDialog(
    stats: EntryStats,
    onStatsRequestToggled: () -> Unit,
) {
    Dialog(
        onDismissRequest = onStatsRequestToggled,
    ) {
        Card {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StatRow()
                StatRow(
                    rowTitle = stringResource(Res.string.stats_row_uploaded_reconciled),
                    statCount = stats.uploadedAndReconciled,
                    applyBackground = true,
                )
                StatRow(
                    rowTitle = stringResource(Res.string.stats_row_uploaded_not_reconciled),
                    statCount = stats.uploadedAndNotReconciled,
                )
                StatRow(
                    rowTitle = stringResource(Res.string.stats_row_not_uploaded_reconciled),
                    statCount = stats.notUploadedAndReconciled,
                    applyBackground = true,
                )
                StatRow(
                    rowTitle = stringResource(Res.string.stats_row_not_uploaded_not_reconciled),
                    statCount = stats.notUploadedAndNotReconciled,
                )
                StatRow(
                    rowTitle = stringResource(Res.string.stats_row_all),
                    statCount = stats.all,
                    applyBackground = true,
                )
            }
        }
    }
}

@Composable
fun StatRow(
    rowTitle: String? = null,
    statCount: EntryStats.Count? = null,
    applyBackground: Boolean = false,
) {
    val background =
        if (applyBackground) {
            Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
        } else {
            Modifier
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(background)
                .padding(4.dp),
    ) {
        if (rowTitle != null) {
            Text(
                text = rowTitle,
                modifier = Modifier.weight(3.5f),
            )
        } else {
            Spacer(modifier = Modifier.weight(3.5f))
        }
        if (statCount != null) {
            Text(
                text = statCount.days,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
            )
            Text(
                text = statCount.entries,
                modifier = Modifier.weight(1.5f),
                textAlign = TextAlign.End,
            )
        } else {
            Text(
                text = stringResource(Res.string.stats_column_days),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
            )
            Text(
                text = stringResource(Res.string.stats_column_entries),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.5f),
                textAlign = TextAlign.End,
            )
        }
    }
}
