package com.ramitsuri.notificationjournal.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Done
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.presentation.theme.NotificationJournalTheme

@Composable
fun WearApp(
    viewState: ViewState,
    onAddRequested: (String) -> Unit,
    onSyncRequested: () -> Unit
) {
    NotificationJournalTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val iconModifier = Modifier
                .size(16.dp)
                .wrapContentSize(align = Alignment.Center)
            val showOnDeviceEntries = viewState.journalEntries.isNotEmpty()
            if (showOnDeviceEntries) {
                val count = viewState.journalEntries.size
                Text(
                    text = pluralStringResource(
                        id = R.plurals.journal_entries_count,
                        count = count,
                        count
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AddButton(onAddRequested = onAddRequested, iconModifier = iconModifier)
                if (showOnDeviceEntries) {
                    SyncButton(onSyncRequested = { onSyncRequested() }, iconModifier = iconModifier)
                }
            }
        }
    }
}

@Composable
fun AddButton(onAddRequested: (String) -> Unit, iconModifier: Modifier) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.processResult(onAddRequested)
        }
    Button(
        modifier = Modifier.size(ButtonDefaults.LargeButtonSize),
        onClick = {
            launcher.launchInputActivity()
        },
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "",
            modifier = iconModifier
        )
    }
}

@Composable
fun SyncButton(
    onSyncRequested: () -> Unit,
    iconModifier: Modifier
) {
    Button(
        modifier = Modifier.size(ButtonDefaults.LargeButtonSize),
        onClick = { onSyncRequested() },
    ) {
        Icon(
            imageVector = Icons.Rounded.Done,
            contentDescription = "",
            modifier = iconModifier
        )
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(viewState = ViewState(), { }, { })
}