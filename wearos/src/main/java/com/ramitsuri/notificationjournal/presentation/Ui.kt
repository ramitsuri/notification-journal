package com.ramitsuri.notificationjournal.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.presentation.theme.NotificationJournalTheme
import java.time.Instant
import java.time.ZoneId

@Composable
fun WearApp(
    viewState: ViewState,
    onAddRequested: (String) -> Unit,
    onTemplateAddRequested: (Int) -> Unit,
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
                viewState.journalEntryTemplates.forEach {
                    item {
                        LargeButton(
                            onClick = { onTemplateAddRequested(it.id) },
                            text = it.text,
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        AddButton(onAddRequested)
                        RequestUploadFromPhoneButton(onUploadRequested)
                    }
                }
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

@Composable
private fun AddButton(onAddRequested: (String) -> Unit) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.processResult(onAddRequested)
        }
    SmallButton(
        onClick = {
            launcher.launchInputActivity()
        },
        contentDescriptionRes = R.string.add_new,
        icon = Icons.Rounded.Add
    )
}

@Composable
private fun TransferToPhoneButton(onTransferRequested: () -> Unit) {
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
private fun RequestUploadFromPhoneButton(onUploadRequested: () -> Unit) {
    SmallButton(
        onClick = onUploadRequested,
        contentDescriptionRes = R.string.upload,
        icon = Icons.Rounded.Upload
    )
}

@Composable
private fun SmallButton(
    onClick: () -> Unit,
    @StringRes contentDescriptionRes: Int,
    icon: ImageVector
) {
    Button(
        modifier = Modifier
            .size(ButtonDefaults.LargeButtonSize),
        onClick = onClick,
    ) {
        val iconModifier = Modifier
            .size(16.dp)
            .wrapContentSize(align = Alignment.Center)
        Icon(
            imageVector = icon,
            contentDescription = stringResource(id = contentDescriptionRes),
            modifier = iconModifier
        )
    }
}

@Composable
private fun LargeButton(
    onClick: () -> Unit,
    text: String,
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        onClick = onClick,
    ) {
        Text(text = text)
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
private fun DefaultPreview() {
    WearApp(viewState = ViewState(), { }, { }, { }, { })
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
private fun JournalEntriesPresentPreview() {
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
    ), { }, { }, { }, { })
}