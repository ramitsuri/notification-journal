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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ramitsuri.notificationjournal.core.model.SortOrder
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.back
import notificationjournal.core.generated.resources.button_text_restart
import notificationjournal.core.generated.resources.button_text_set_server
import notificationjournal.core.generated.resources.cancel
import notificationjournal.core.generated.resources.error
import notificationjournal.core.generated.resources.ok
import notificationjournal.core.generated.resources.settings_server_title
import notificationjournal.core.generated.resources.settings_sort_order_asc
import notificationjournal.core.generated.resources.settings_sort_order_desc
import notificationjournal.core.generated.resources.settings_sort_order_title
import notificationjournal.core.generated.resources.settings_tags_subtitle
import notificationjournal.core.generated.resources.settings_tags_title
import notificationjournal.core.generated.resources.settings_templates_subtitle
import notificationjournal.core.generated.resources.settings_templates_title
import notificationjournal.core.generated.resources.settings_upload_subtitle
import notificationjournal.core.generated.resources.settings_upload_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    state: SettingsViewState,
    onBack: () -> Unit,
    onUploadClicked: () -> Unit,
    onApiUrlSet: (String) -> Unit,
    onSortOrderClicked: () -> Unit,
    onErrorAcknowledged: () -> Unit,
    onTagsClicked: () -> Unit,
    onTemplatesClicked: () -> Unit,
    shutdown: () -> Unit,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var serverSet by rememberSaveable { mutableStateOf(false) }

    if (showDialog) {
        SetApiUrlDialog(
            state.serverText,
            onPositiveClick = { value ->
                showDialog = !showDialog
                onApiUrlSet(value)
                serverSet = true
            },
            onNegativeClick = {
                showDialog = !showDialog
            }
        )
    }
    Surface {
        Column(
            modifier = modifier
                .fillMaxSize()
                .systemBarsPadding()
                .displayCutoutPadding(),
        ) {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.back)
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                item {
                    val subtitle = when (state.serverState) {
                        ServerState.RESTART -> {
                            stringResource(Res.string.button_text_restart)
                        }

                        ServerState.SET_SERVER -> {
                            stringResource(Res.string.button_text_set_server)
                        }

                        ServerState.SERVER_SET -> {
                            state.serverText
                        }
                    }
                    SettingsItem(
                        title = stringResource(Res.string.settings_server_title),
                        subtitle = subtitle,
                        onClick = {
                            when (state.serverState) {
                                ServerState.RESTART -> {
                                    shutdown()
                                }

                                ServerState.SET_SERVER -> {
                                    showDialog = true
                                }

                                ServerState.SERVER_SET -> {
                                    showDialog = true
                                }
                            }
                        },
                        showProgress = false
                    )
                }
                item {
                    SettingsItem(
                        title = stringResource(Res.string.settings_tags_title),
                        subtitle = stringResource(Res.string.settings_tags_subtitle),
                        onClick = onTagsClicked,
                        showProgress = false,
                        modifier = modifier
                    )
                }
                item {
                    SettingsItem(
                        title = stringResource(Res.string.settings_templates_title),
                        subtitle = stringResource(Res.string.settings_templates_subtitle),
                        onClick = onTemplatesClicked,
                        showProgress = false
                    )
                }
                item {
                    SettingsItem(
                        title = stringResource(Res.string.settings_upload_title),
                        subtitle = stringResource(Res.string.settings_upload_subtitle),
                        onClick = onUploadClicked,
                        showProgress = state.uploadLoading,
                        modifier = modifier
                    )
                }
                item {
                    SettingsItem(
                        title = stringResource(Res.string.settings_sort_order_title),
                        subtitle = when (state.sortOrder) {
                            SortOrder.ASC -> stringResource(Res.string.settings_sort_order_asc)
                            SortOrder.DESC -> stringResource(Res.string.settings_sort_order_desc)
                        },
                        onClick = onSortOrderClicked,
                        showProgress = false
                    )
                }
            }

            state.error?.let { error ->
                ErrorAlert(text = error, onDismiss = onErrorAcknowledged)
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showProgress: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(onClick = onClick, enabled = !showProgress)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(4.dp)
        )
        if (showProgress) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun SetApiUrlDialog(
    previousText: String,
    onPositiveClick: (String) -> Unit,
    onNegativeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by rememberSaveable { mutableStateOf(previousText.ifEmpty { "http://" }) }
    Dialog(onDismissRequest = { }) {
        Card {
            Column(modifier = modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = text,
                    singleLine = true,
                    onValueChange = { text = it },
                    modifier = modifier.fillMaxWidth()
                )
                Spacer(modifier = modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = {
                        onNegativeClick()
                    }) {
                        Text(text = stringResource(Res.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        onPositiveClick(text)
                    }) {
                        Text(text = stringResource(Res.string.ok))
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorAlert(text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.error),
                style = MaterialTheme.typography.titleSmall,
            )
        },
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(Res.string.ok),
                )
            }
        }
    )
}
