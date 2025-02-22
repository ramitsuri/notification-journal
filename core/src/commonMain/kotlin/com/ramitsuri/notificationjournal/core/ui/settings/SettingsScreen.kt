package com.ramitsuri.notificationjournal.core.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ramitsuri.notificationjournal.core.ui.components.Toolbar
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.cancel
import notificationjournal.core.generated.resources.data_host
import notificationjournal.core.generated.resources.device_name
import notificationjournal.core.generated.resources.exchange_name
import notificationjournal.core.generated.resources.ok
import notificationjournal.core.generated.resources.password
import notificationjournal.core.generated.resources.settings_app_version
import notificationjournal.core.generated.resources.settings_copy_with_empty_tags
import notificationjournal.core.generated.resources.settings_data_sharing_not_set
import notificationjournal.core.generated.resources.settings_data_sharing_title
import notificationjournal.core.generated.resources.settings_delete_all_subtitle
import notificationjournal.core.generated.resources.settings_delete_all_title
import notificationjournal.core.generated.resources.settings_journal_import_subtitle
import notificationjournal.core.generated.resources.settings_journal_import_title
import notificationjournal.core.generated.resources.settings_logs
import notificationjournal.core.generated.resources.settings_showConflictDiffInline
import notificationjournal.core.generated.resources.settings_show_empty_tags
import notificationjournal.core.generated.resources.settings_tags_subtitle
import notificationjournal.core.generated.resources.settings_tags_title
import notificationjournal.core.generated.resources.settings_templates_subtitle
import notificationjournal.core.generated.resources.settings_templates_title
import notificationjournal.core.generated.resources.settings_upload_subtitle
import notificationjournal.core.generated.resources.settings_upload_title
import notificationjournal.core.generated.resources.username
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    state: SettingsViewState,
    onBack: () -> Unit,
    onUploadClicked: () -> Unit,
    onDataSharingPropertiesSet: (DataHost, ExchangeName, DeviceName, Username, Password) -> Unit,
    onTagsClicked: () -> Unit,
    onTemplatesClicked: () -> Unit,
    onToggleShowConflictDiffInline: () -> Unit,
    onToggleShowEmptyTags: () -> Unit,
    onToggleCopyWithEmptyTags: () -> Unit,
    onLogsClicked: () -> Unit,
    onJournalImportClicked: () -> Unit,
    onDeleteAll: () -> Unit,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    if (showDialog) {
        DataSharingPropertiesDialog(
            dataHost = state.dataHost,
            exchangeName = state.exchangeName,
            deviceName = state.deviceName,
            username = state.username,
            password = state.password,
            onPositiveClick = { dataHost, exchangeName, deviceName, username, password ->
                showDialog = !showDialog
                onDataSharingPropertiesSet(dataHost, exchangeName, deviceName, username, password)
            },
            onNegativeClick = { showDialog = !showDialog },
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
                            if (state.dataHost.host.isNotEmpty()) {
                                append(state.dataHost.host)
                                append(" : ")
                            }
                            if (state.exchangeName.name.isNotEmpty()) {
                                append(state.exchangeName.name)
                                append(" : ")
                            }
                            if (state.deviceName.name.isNotEmpty()) {
                                append(state.deviceName.name)
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
                            title = stringResource(Res.string.settings_journal_import_title),
                            subtitle = stringResource(Res.string.settings_journal_import_subtitle),
                            onClick = onJournalImportClicked,
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
    dataHost: DataHost,
    exchangeName: ExchangeName,
    deviceName: DeviceName,
    username: Username,
    password: Password,
    onPositiveClick: (DataHost, ExchangeName, DeviceName, Username, Password) -> Unit,
    onNegativeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dataHostText by rememberSaveable { mutableStateOf(dataHost.host) }
    var exchangeNameText by rememberSaveable { mutableStateOf(exchangeName.name) }
    var deviceNameText by rememberSaveable { mutableStateOf(deviceName.name) }
    var usernameText by rememberSaveable { mutableStateOf(username.username) }
    var passwordText by rememberSaveable { mutableStateOf(password.password) }

    Dialog(onDismissRequest = { }) {
        Card {
            Column(modifier = modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = dataHostText,
                    singleLine = true,
                    label = {
                        Text(stringResource(Res.string.data_host))
                    },
                    onValueChange = { dataHostText = it },
                    modifier = modifier.fillMaxWidth(),
                )
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
                OutlinedTextField(
                    value = usernameText,
                    singleLine = true,
                    label = {
                        Text(stringResource(Res.string.username))
                    },
                    onValueChange = { usernameText = it },
                    modifier = modifier.fillMaxWidth(),
                )
                Spacer(modifier = modifier.height(16.dp))
                OutlinedTextField(
                    value = passwordText,
                    singleLine = true,
                    label = {
                        Text(stringResource(Res.string.password))
                    },
                    onValueChange = { passwordText = it },
                    modifier = modifier.fillMaxWidth(),
                )
                Spacer(modifier = modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = modifier.fillMaxWidth(),
                ) {
                    TextButton(onClick = {
                        onNegativeClick()
                    }) {
                        Text(text = stringResource(Res.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onPositiveClick(
                                DataHost(dataHostText),
                                ExchangeName(exchangeNameText),
                                DeviceName(deviceNameText),
                                Username(usernameText),
                                Password(passwordText),
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
