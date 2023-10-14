package com.ramitsuri.notificationjournal.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.scrollAway
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.core.model.JournalEntry
import com.ramitsuri.notificationjournal.presentation.theme.NotificationJournalTheme
import java.time.Instant
import java.time.ZoneId

@Composable
fun WearApp(
    viewState: ViewState,
    onAddRequested: (String) -> Unit,
    onUploadRequested: () -> Unit,
    onTransferRequested: () -> Unit
) {
    NotificationJournalTheme {
        val listState = rememberScalingLazyListState()
        Scaffold(
            timeText = {
                TimeText(modifier = Modifier.scrollAway(listState))
            },
            vignette = {
                Vignette(vignettePosition = VignettePosition.TopAndBottom)
            },
            positionIndicator = {
                PositionIndicator(
                    scalingLazyListState = listState
                )
            }
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                autoCentering = AutoCenteringParams(itemIndex = 0),
                state = listState
            ) {
                val showOnDeviceEntries = viewState.journalEntries.isNotEmpty()
                if (showOnDeviceEntries) {
                    val count = viewState.journalEntries.size
                    item {
                        Text(
                            text = pluralStringResource(
                                id = R.plurals.journal_entries_count,
                                count = count,
                                count
                            )
                        )
                    }
                }
                item { AddButton(onAddRequested) }
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { RequestUploadFromPhoneButton(onUploadRequested) }
                if (showOnDeviceEntries) {
                    item { TransferToPhoneButton(onTransferRequested) }
                }
            }
        }
    }
}

@Composable
fun AddButton(onAddRequested: (String) -> Unit) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.processResult(onAddRequested)
        }
    Button(
        modifier = Modifier
            .size(ButtonDefaults.LargeButtonSize),
        onClick = {
            launcher.launchInputActivity()
        },
    ) {
        val iconModifier = Modifier
            .size(16.dp)
            .wrapContentSize(align = Alignment.Center)
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = stringResource(id = R.string.add_new),
            modifier = iconModifier
        )
    }
}

@Composable
fun TransferToPhoneButton(onTransferRequested: () -> Unit) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        onClick = { onTransferRequested() },
    ) {
        Text(text = stringResource(id = R.string.transfer_to_phone))
    }
}

@Composable
fun RequestUploadFromPhoneButton(onUploadRequested: () -> Unit) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        onClick = { onUploadRequested() },
    ) {
        Text(text = stringResource(id = R.string.upload))
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(viewState = ViewState(), { }, { }, { })
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun JournalEntriesPresentPreview() {
    WearApp(viewState = ViewState(
        journalEntries = listOf(
            JournalEntry(
                id = 0,
                entryTime = Instant.now(),
                timeZone = ZoneId.systemDefault(),
                text = "Text1"
            ),
            JournalEntry(
                id = 0,
                entryTime = Instant.now(),
                timeZone = ZoneId.systemDefault(),
                text = "Text2"
            ),
            JournalEntry(
                id = 0,
                entryTime = Instant.now(),
                timeZone = ZoneId.systemDefault(),
                text = "Text3"
            )
        )
    ), { }, { }, { })
}