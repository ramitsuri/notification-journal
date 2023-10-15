package com.ramitsuri.notificationjournal.ui.settings

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.core.model.SortOrder
import com.ramitsuri.notificationjournal.core.utils.shutdown

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
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
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
    val context = LocalContext.current

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
                Icons.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.back)
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            item {
                val subtitle = when (state.serverState) {
                    ServerState.RESTART -> {
                        stringResource(id = R.string.button_text_restart)
                    }

                    ServerState.SET_SERVER -> {
                        stringResource(id = R.string.button_text_set_server)
                    }

                    ServerState.SERVER_SET -> {
                        state.serverText
                    }
                }
                SettingsItem(
                    title = stringResource(id = R.string.settings_server_title),
                    subtitle = subtitle,
                    onClick = {
                        when (state.serverState) {
                            ServerState.RESTART -> {
                                context.shutdown()
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
                    title = stringResource(id = R.string.settings_tags_title),
                    subtitle = stringResource(id = R.string.settings_tags_subtitle),
                    onClick = onTagsClicked,
                    showProgress = false,
                    modifier = modifier
                )
            }
            item {
                SettingsItem(
                    title = stringResource(id = R.string.settings_upload_title),
                    subtitle = stringResource(id = R.string.settings_upload_subtitle),
                    onClick = onUploadClicked,
                    showProgress = state.uploadLoading,
                    modifier = modifier
                )
            }
            item {
                SettingsItem(
                    title = stringResource(id = R.string.settings_sort_order_title),
                    subtitle = when (state.sortOrder) {
                        SortOrder.ASC -> stringResource(id = R.string.settings_sort_order_asc)
                        SortOrder.DESC -> stringResource(id = R.string.settings_sort_order_desc)
                    },
                    onClick = onSortOrderClicked,
                    showProgress = false
                )
            }
        }

        state.error?.let { error ->
            LaunchedEffect(error) {
                snackbarHostState.showSnackbar(error)
                onErrorAcknowledged()
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
                        Text(text = stringResource(id = R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(onClick = {
                        onPositiveClick(text)
                    }) {
                        Text(text = stringResource(id = R.string.ok))
                    }
                }
            }
        }
    }
}
