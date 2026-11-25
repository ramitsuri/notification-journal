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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ramitsuri.notificationjournal.core.BuildKonfig
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.model.ForceUploadAllStatus
import com.ramitsuri.notificationjournal.core.model.WorkProfile
import com.ramitsuri.notificationjournal.core.model.stats.EntryStats
import com.ramitsuri.notificationjournal.core.ui.components.Toolbar
import com.ramitsuri.notificationjournal.core.work.WorkBridgeController
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.cancel
import notificationjournal.core.generated.resources.close
import notificationjournal.core.generated.resources.data_host
import notificationjournal.core.generated.resources.device_name
import notificationjournal.core.generated.resources.encryption_password
import notificationjournal.core.generated.resources.encryption_salt
import notificationjournal.core.generated.resources.export_directory_dialog_title
import notificationjournal.core.generated.resources.export_directory_input_label
import notificationjournal.core.generated.resources.font_size
import notificationjournal.core.generated.resources.ip_address
import notificationjournal.core.generated.resources.more
import notificationjournal.core.generated.resources.ok
import notificationjournal.core.generated.resources.settings_ai_diagnostics_subtitle
import notificationjournal.core.generated.resources.settings_ai_diagnostics_title
import notificationjournal.core.generated.resources.settings_app_version
import notificationjournal.core.generated.resources.settings_copy_with_empty_tags
import notificationjournal.core.generated.resources.settings_data_sharing_not_set
import notificationjournal.core.generated.resources.settings_data_sharing_title
import notificationjournal.core.generated.resources.settings_delete_all_subtitle
import notificationjournal.core.generated.resources.settings_delete_all_title
import notificationjournal.core.generated.resources.settings_force_upload_all_done
import notificationjournal.core.generated.resources.settings_force_upload_all_initial
import notificationjournal.core.generated.resources.settings_force_upload_all_nothing
import notificationjournal.core.generated.resources.settings_force_upload_all_subtitle
import notificationjournal.core.generated.resources.settings_force_upload_all_title
import notificationjournal.core.generated.resources.settings_force_upload_all_uploading
import notificationjournal.core.generated.resources.settings_journal_export_subtitle
import notificationjournal.core.generated.resources.settings_journal_export_title
import notificationjournal.core.generated.resources.settings_journal_import_subtitle
import notificationjournal.core.generated.resources.settings_journal_import_title
import notificationjournal.core.generated.resources.settings_logs
import notificationjournal.core.generated.resources.settings_showConflictDiffInline
import notificationjournal.core.generated.resources.settings_show_empty_tags
import notificationjournal.core.generated.resources.settings_show_stats_title
import notificationjournal.core.generated.resources.settings_tags_subtitle
import notificationjournal.core.generated.resources.settings_tags_title
import notificationjournal.core.generated.resources.settings_templates_subtitle
import notificationjournal.core.generated.resources.settings_templates_title
import notificationjournal.core.generated.resources.settings_work_mac_connect
import notificationjournal.core.generated.resources.settings_work_mac_connected
import notificationjournal.core.generated.resources.settings_work_mac_connecting
import notificationjournal.core.generated.resources.settings_work_mac_dialog_desc
import notificationjournal.core.generated.resources.settings_work_mac_not_paired
import notificationjournal.core.generated.resources.settings_work_mac_repair
import notificationjournal.core.generated.resources.settings_work_mac_start
import notificationjournal.core.generated.resources.settings_work_mac_stop
import notificationjournal.core.generated.resources.settings_work_mac_stopped
import notificationjournal.core.generated.resources.settings_work_mac_title
import notificationjournal.core.generated.resources.settings_work_mac_unpair
import notificationjournal.core.generated.resources.stats_column_days
import notificationjournal.core.generated.resources.stats_column_entries
import notificationjournal.core.generated.resources.stats_row_all
import notificationjournal.core.generated.resources.stats_row_not_uploaded_not_reconciled
import notificationjournal.core.generated.resources.stats_row_not_uploaded_reconciled
import notificationjournal.core.generated.resources.stats_row_uploaded_not_reconciled
import notificationjournal.core.generated.resources.stats_row_uploaded_reconciled
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    state: SettingsViewState,
    onBack: () -> Unit,
    onForceUploadAllClicked: () -> Unit,
    onDataSharingPropertiesSet: (DataHost, DeviceName) -> Unit,
    onTagsClicked: () -> Unit,
    onTemplatesClicked: () -> Unit,
    onToggleShowConflictDiffInline: () -> Unit,
    onToggleShowEmptyTags: () -> Unit,
    onToggleCopyWithEmptyTags: () -> Unit,
    onLogsClicked: () -> Unit,
    onJournalImportClicked: () -> Unit,
    onDeleteAll: () -> Unit,
    onShowStatsToggled: () -> Unit,
    onExportDirectorySet: (String) -> Unit,
    onIncreaseAddEditFontSize: () -> Unit,
    onDecreaseAddEditFontSize: () -> Unit,
    onQrCodeScanned: (String) -> Unit,
    onAiDiagnosticsClicked: (() -> Unit)? = null,
    onApplyDummySyncConfig: (() -> Unit)? = null,
    onStartWork: (() -> Unit)? = null,
    onStopWork: (() -> Unit)? = null,
    onRepairWork: (() -> Unit)? = null,
    onUnpairWork: (() -> Unit)? = null,
    workBridgeState: WorkBridgeController.State = WorkBridgeController.State.Stopped,
) {
    var showDataSharingPropertiesDialog by rememberSaveable { mutableStateOf(false) }
    var showWorkMacDialog by rememberSaveable { mutableStateOf(false) }
    var showForceUploadAllDialog by rememberSaveable { mutableStateOf(false) }
    var showExportDirectoryDialog by rememberSaveable { mutableStateOf(false) }

    if (showWorkMacDialog) {
        WorkMacDialog(
            workProfile = state.workProfile,
            workBridgeState = workBridgeState,
            onStartWork = {
                onStartWork?.invoke()
            },
            onStopWork = {
                onStopWork?.invoke()
            },
            onRepairWork = {
                onRepairWork?.invoke()
            },
            onUnpairWork = {
                onUnpairWork?.invoke()
            },
            onDismissRequest = { showWorkMacDialog = false },
        )
    }

    if (showDataSharingPropertiesDialog) {
        DataSharingPropertiesDialog(
            dataHostProperties = state.dataHostProperties,
            ipAddress = state.ipAddress,
            encryptionSalt = state.encryptionSalt,
            encryptionPassword = state.encryptionPassword,
            qrCodeContent = state.qrCodeContent,
            actsAsServer = state.actsAsServer,
            onApplyDummySyncConfig = onApplyDummySyncConfig,
            onPositiveClick = { dataHost, deviceName ->
                showDataSharingPropertiesDialog = !showDataSharingPropertiesDialog
                onDataSharingPropertiesSet(dataHost, deviceName)
            },
            onNegativeClick = { showDataSharingPropertiesDialog = !showDataSharingPropertiesDialog },
            onQrCodeScanned = {
                showDataSharingPropertiesDialog = false
                onQrCodeScanned(it)
            },
        )
    }
    if (showForceUploadAllDialog) {
        ForceUploadAllStatusDialog(
            forceUploadAllStatus = state.forceUploadStatus,
            onStart = {
                onForceUploadAllClicked()
            },
            onCancel = { showForceUploadAllDialog = false },
            onDone = { showForceUploadAllDialog = false },
        )
    }
    if (showExportDirectoryDialog) {
        ExportDirectoryDialog(
            currentDirectory = state.exportDirectory ?: "",
            onDirectorySet = { exportDirectory ->
                showExportDirectoryDialog = !showExportDirectoryDialog
                onExportDirectorySet(exportDirectory)
            },
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
                            if (state.dataHostProperties.deviceName.isNotEmpty()) {
                                append(state.dataHostProperties.deviceName)
                            }
                        }
                    SettingsItem(
                        title = stringResource(Res.string.settings_data_sharing_title),
                        subtitle = subtitle.ifEmpty { stringResource(Res.string.settings_data_sharing_not_set) },
                        onClick = {
                            showDataSharingPropertiesDialog = true
                        },
                        showProgress = false,
                    )
                }
                if (onStartWork != null) {
                    item {
                        val subtitle =
                            if (state.workProfile == null) {
                                stringResource(Res.string.settings_work_mac_not_paired)
                            } else {
                                val endpoint = "${state.workProfile.host}:${state.workProfile.port}"
                                when (workBridgeState) {
                                    is WorkBridgeController.State.Connected ->
                                        stringResource(Res.string.settings_work_mac_connected, endpoint)

                                    is WorkBridgeController.State.Connecting ->
                                        stringResource(Res.string.settings_work_mac_connecting, endpoint)

                                    is WorkBridgeController.State.Stopped,
                                    is WorkBridgeController.State.Idle ->
                                        stringResource(Res.string.settings_work_mac_stopped, endpoint)
                                }
                            }
                        SettingsItem(
                            title = stringResource(Res.string.settings_work_mac_title),
                            subtitle = subtitle,
                            onClick = {
                                showWorkMacDialog = true
                            },
                            showProgress = false,
                        )
                    }
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
                    AddEditFontSizeItem(
                        fontSize = state.addEditFontSize,
                        onIncreaseClick = onIncreaseAddEditFontSize,
                        onDecreaseClick = onDecreaseAddEditFontSize,
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
                        title = stringResource(Res.string.settings_force_upload_all_title),
                        subtitle = stringResource(Res.string.settings_force_upload_all_subtitle),
                        onClick = { showForceUploadAllDialog = true },
                        showProgress = false,
                    )
                }
                if (state.showJournalImportButton) {
                    item {
                        SettingsItem(
                            title = stringResource(Res.string.settings_journal_import_title),
                            subtitle = stringResource(Res.string.settings_journal_import_subtitle),
                            onClick = onJournalImportClicked,
                            showProgress = false,
                        )
                    }
                }
                val exportDirectory = state.exportDirectory
                if (exportDirectory != null) {
                    item {
                        SettingsItem(
                            title = stringResource(Res.string.settings_journal_export_title),
                            subtitle =
                                exportDirectory.ifBlank {
                                    stringResource(Res.string.settings_journal_export_subtitle)
                                },
                            onClick = { showExportDirectoryDialog = true },
                            showProgress = false,
                        )
                    }
                }
                if (onAiDiagnosticsClicked != null) {
                    item {
                        SettingsItem(
                            title = stringResource(Res.string.settings_ai_diagnostics_title),
                            subtitle = stringResource(Res.string.settings_ai_diagnostics_subtitle),
                            onClick = onAiDiagnosticsClicked,
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
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String = "",
    onClick: () -> Unit,
    showProgress: Boolean,
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
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
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
private fun AddEditFontSizeItem(
    modifier: Modifier = Modifier,
    fontSize: Int,
    onIncreaseClick: () -> Unit,
    onDecreaseClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = stringResource(Res.string.font_size),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(4.dp),
            )
            Text(
                text = fontSize.toString(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onDecreaseClick) {
            Text(text = "-")
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(onClick = onIncreaseClick) {
            Text(text = "+")
        }
    }
}

@Composable
private fun ForceUploadAllStatusDialog(
    forceUploadAllStatus: ForceUploadAllStatus,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    Dialog(onDismissRequest = { }) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                when (forceUploadAllStatus) {
                    is ForceUploadAllStatus.Initial -> {
                        Text(stringResource(Res.string.settings_force_upload_all_initial))
                        DialogButtonRow(
                            showCancel = true,
                            onPositiveClick = onStart,
                            onNegativeClick = onCancel,
                        )
                    }

                    is ForceUploadAllStatus.NothingToUpload -> {
                        Text(stringResource(Res.string.settings_force_upload_all_nothing))
                        DialogButtonRow(
                            showCancel = false,
                            onPositiveClick = onDone,
                            onNegativeClick = { },
                        )
                    }

                    is ForceUploadAllStatus.Uploading -> {
                        Text(
                            stringResource(
                                Res.string.settings_force_upload_all_uploading,
                                forceUploadAllStatus.totalToUploadDaysCount,
                                forceUploadAllStatus.uploadedDaysCount,
                                forceUploadAllStatus.failedDaysCount,
                            ),
                        )
                    }

                    is ForceUploadAllStatus.Done -> {
                        Text(
                            stringResource(
                                Res.string.settings_force_upload_all_done,
                                forceUploadAllStatus.totalToUploadDaysCount,
                                forceUploadAllStatus.uploadedDaysCount,
                                forceUploadAllStatus.failedDaysCount,
                            ),
                        )
                        DialogButtonRow(
                            showCancel = false,
                            onPositiveClick = onDone,
                            onNegativeClick = { },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DataSharingPropertiesDialog(
    dataHostProperties: DataHostProperties,
    qrCodeContent: String?,
    actsAsServer: Boolean,
    ipAddress: String?,
    encryptionPassword: String?,
    encryptionSalt: String?,
    onQrCodeScanned: (String) -> Unit,
    onPositiveClick: (DataHost, DeviceName) -> Unit,
    onNegativeClick: () -> Unit,
    onApplyDummySyncConfig: (() -> Unit)? = null,
) {
    var dataHostText by rememberSaveable(dataHostProperties.dataHost) {
        mutableStateOf(dataHostProperties.dataHost)
    }
    var deviceNameText by rememberSaveable(dataHostProperties.deviceName) {
        mutableStateOf(dataHostProperties.deviceName)
    }

    Dialog(onDismissRequest = onNegativeClick) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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
                        modifier = Modifier.weight(1f),
                    )
                    OtherHostsButton(
                        otherHosts = dataHostProperties.otherHosts,
                        otherHostPicked = {
                            dataHostText = it
                        },
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (qrCodeContent != null && actsAsServer) {
                    QrCodeImage(
                        content = qrCodeContent,
                        modifier = Modifier.size(256.dp),
                    )
                } else {
                    QrCodeScanner(
                        modifier =
                            Modifier
                                .sizeIn(maxHeight = 400.dp),
                        onQrCodeScanned = onQrCodeScanned,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.encryption_password, encryptionPassword.toString()),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.encryption_salt, encryptionSalt.toString()),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (ipAddress != null && actsAsServer) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.ip_address, ipAddress),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (BuildKonfig.IS_DEBUG && onApplyDummySyncConfig != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onApplyDummySyncConfig,
                    ) {
                        Text("Use Test / Dummy Sync Values")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                DialogButtonRow(
                    showCancel = true,
                    onPositiveClick = {
                        onPositiveClick(
                            DataHost(dataHostText),
                            DeviceName(deviceNameText),
                        )
                    },
                    onNegativeClick = onNegativeClick,
                )
            }
        }
    }
}

@Composable
private fun DialogButtonRow(
    showCancel: Boolean,
    onPositiveClick: () -> Unit,
    onNegativeClick: () -> Unit,
) {
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (showCancel) {
            TextButton(
                onClick = {
                    onNegativeClick()
                },
            ) {
                Text(text = stringResource(Res.string.cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        TextButton(
            onClick = onPositiveClick,
        ) {
            Text(text = stringResource(Res.string.ok))
        }
    }
}

@Composable
private fun OtherHostsButton(
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
                    Modifier
                        .fillMaxWidth()
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
private fun StatRow(
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

@Composable
private fun ExportDirectoryDialog(
    currentDirectory: String,
    onDirectorySet: (String) -> Unit,
) {
    var text by remember { mutableStateOf(currentDirectory) }
    AlertDialog(
        onDismissRequest = { onDirectorySet(text) },
        title = { Text(stringResource(Res.string.export_directory_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(Res.string.export_directory_input_label)) },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDirectorySet(text)
                },
            ) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDirectorySet(text)
                },
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

@Composable
private fun WorkMacDialog(
    workProfile: WorkProfile?,
    workBridgeState: WorkBridgeController.State,
    onStartWork: () -> Unit,
    onStopWork: () -> Unit,
    onRepairWork: () -> Unit,
    onUnpairWork: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = stringResource(Res.string.settings_work_mac_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (workProfile == null) {
                    Text(
                        text = stringResource(Res.string.settings_work_mac_dialog_desc),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text(stringResource(Res.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onDismissRequest()
                                onStartWork()
                            },
                        ) {
                            Text(stringResource(Res.string.settings_work_mac_start))
                        }
                    }
                } else {
                    val statusText =
                        when (workBridgeState) {
                            is WorkBridgeController.State.Connected ->
                                stringResource(
                                    Res.string.settings_work_mac_connected,
                                    "${workProfile.host}:${workProfile.port}",
                                )

                            is WorkBridgeController.State.Connecting ->
                                "Connecting… (${workBridgeState.attempt}/${workBridgeState.maxAttempts})"

                            is WorkBridgeController.State.Stopped,
                            is WorkBridgeController.State.Idle ->
                                stringResource(
                                    Res.string.settings_work_mac_stopped,
                                    "${workProfile.host}:${workProfile.port}",
                                )
                        }
                    Text(
                        text = "Host: ${workProfile.host}:${workProfile.port}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Status: $statusText",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val isRunning =
                            workBridgeState is WorkBridgeController.State.Connected ||
                                workBridgeState is WorkBridgeController.State.Connecting
                        if (isRunning) {
                            OutlinedButton(
                                onClick = onStopWork,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(Res.string.settings_work_mac_stop))
                            }
                        } else {
                            Button(
                                onClick = onStartWork,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(Res.string.settings_work_mac_connect))
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                onDismissRequest()
                                onRepairWork()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(Res.string.settings_work_mac_repair))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                onDismissRequest()
                                onUnpairWork()
                            },
                        ) {
                            Text(
                                text = stringResource(Res.string.settings_work_mac_unpair),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        TextButton(onClick = onDismissRequest) {
                            Text(stringResource(Res.string.close))
                        }
                    }
                }
            }
        }
    }
}

