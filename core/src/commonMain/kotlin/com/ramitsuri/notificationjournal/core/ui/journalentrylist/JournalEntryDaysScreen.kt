package com.ramitsuri.notificationjournal.core.ui.journalentrylist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LabelOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ramitsuri.notificationjournal.core.model.DateWithCount
import com.ramitsuri.notificationjournal.core.ui.theme.greenColor
import com.ramitsuri.notificationjournal.core.ui.theme.redColor
import com.ramitsuri.notificationjournal.core.utils.dayOfWeek
import com.ramitsuri.notificationjournal.core.utils.monthDateYear
import kotlinx.datetime.LocalDate
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.add_entry_content_description
import notificationjournal.core.generated.resources.cancel
import notificationjournal.core.generated.resources.entry_days_empty
import notificationjournal.core.generated.resources.not_verified
import notificationjournal.core.generated.resources.proceed
import notificationjournal.core.generated.resources.reconcile_all
import notificationjournal.core.generated.resources.reconcile_all_body
import notificationjournal.core.generated.resources.reconcile_all_upload_on_success
import notificationjournal.core.generated.resources.search
import notificationjournal.core.generated.resources.settings
import notificationjournal.core.generated.resources.sync_up
import notificationjournal.core.generated.resources.verifying
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun JournalEntryDaysScreen(
    state: ViewState,
    showToolbarActions: Boolean,
    showFloatingActionButton: Boolean,
    onAddClicked: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onSettingsClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onResetReceiveHelper: () -> Unit,
    onViewByDate: () -> Unit,
    onSyncClicked: () -> Unit,
    onReconcileAll: (uploadOnSuccess: Boolean) -> Unit,
) {
    var showReconcileAllConfirmation by remember { mutableStateOf(false) }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .focusable()
                .onKeyEvent {
                    if (
                        it.isMetaPressed &&
                        it.key == Key.N &&
                        it.type == KeyEventType.KeyUp
                    ) {
                        onAddClicked()
                        true
                    } else if (
                        it.isMetaPressed &&
                        it.key == Key.Comma &&
                        it.type == KeyEventType.KeyUp
                    ) {
                        onSettingsClicked()
                        true
                    } else if (it.key == Key.S &&
                        it.type == KeyEventType.KeyDown
                    ) {
                        onSyncClicked()
                        true
                    } else {
                        false
                    }
                },
        floatingActionButton = {
            if (showFloatingActionButton) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 32.dp),
                    onClick = onAddClicked,
                ) {
                    Icon(
                        Icons.Filled.Add,
                        stringResource(Res.string.add_entry_content_description),
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
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
            val scrollBehavior =
                TopAppBarDefaults.enterAlwaysScrollBehavior(
                    rememberTopAppBarState(),
                )
            Toolbar(
                scrollBehavior = scrollBehavior,
                isConnected = state.isConnected,
                notUploadedCount = state.notUploadedCount,
                showToolbarActions = showToolbarActions,
                onResetReceiveHelper = onResetReceiveHelper,
                onViewByDate = onViewByDate,
                onSearchClicked = onSearchClicked,
                onSettingsClicked = onSettingsClicked,
                onSyncClicked = onSyncClicked,
            )
            List(
                selectedDate = state.selectedDate,
                dateWithCountList = state.dateWithCountList,
                todayDate = state.todayDate,
                onClick = onDateSelected,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
            )
            TextButton(
                onClick = {
                    showReconcileAllConfirmation = true
                },
            ) {
                Text(text = stringResource(Res.string.reconcile_all))
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
        ReconcileAllConfirmationDialog(
            showReconcileAllConfirmation = showReconcileAllConfirmation,
            onDismiss = { showReconcileAllConfirmation = false },
            onReconcileAll = {
                showReconcileAllConfirmation = false
                onReconcileAll(it)
            },
        )
    }
}

@Composable
private fun List(
    modifier: Modifier,
    selectedDate: LocalDate?,
    todayDate: LocalDate?,
    dateWithCountList: List<DateWithCount>,
    onClick: (LocalDate) -> Unit,
) {
    if (dateWithCountList.isEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.entry_days_empty))
        }
    } else {
        LazyColumn(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(
                items = dateWithCountList,
                key = { it.date.toString() },
            ) { dateWithCount ->
                DateWithCountItem(
                    dateWithCount = dateWithCount,
                    isSelected = dateWithCount.date == selectedDate,
                    isToday = dateWithCount.date == todayDate,
                    onClick = { onClick(dateWithCount.date) },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}

@Composable
private fun DateWithCountItem(
    dateWithCount: DateWithCount,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .clickable(
                    onClick = onClick,
                )
                .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            // Date
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(monthDateYear(toFormat = dateWithCount.date))
                Spacer(modifier = Modifier.width(4.dp))
                if (isToday) {
                    Icon(
                        imageVector = Icons.Outlined.Today,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Verification
                TooltipContainer(label = dateWithCount.verification.label()) {
                    dateWithCount.verification.Icon()
                }
                Spacer(modifier = Modifier.width(4.dp))
                // Day of week
                Text(
                    dayOfWeek(toFormat = dateWithCount.date),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (dateWithCount.untaggedCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.LabelOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = dateWithCount.untaggedCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            if (dateWithCount.conflictCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = dateWithCount.conflictCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun TooltipContainer(
    label: String,
    content: @Composable () -> Unit,
) {
    val state = rememberTooltipState()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(label)
            }
        },
        state = state,
    ) {
        content()
    }
}

@Composable
private fun DateWithCount.Verification.label(): String {
    return when (this) {
        is DateWithCount.Verification.InProgress -> {
            stringResource(Res.string.verifying)
        }

        is DateWithCount.Verification.NotVerified -> {
            stringResource(Res.string.not_verified)
        }

        is DateWithCount.Verification.Verified -> {
            with
        }
    }
}

@Composable
private fun DateWithCount.Verification.Icon() {
    val (icon, tint) =
        when (this) {
            is DateWithCount.Verification.InProgress -> {
                Icons.Default.Pending to LocalContentColor.current
            }

            is DateWithCount.Verification.NotVerified -> {
                Icons.Default.Cancel to redColor
            }

            is DateWithCount.Verification.Verified -> {
                Icons.Default.CheckCircle to greenColor
            }
        }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(16.dp),
    )
}

@Composable
private fun Toolbar(
    scrollBehavior: TopAppBarScrollBehavior? = null,
    isConnected: Boolean,
    notUploadedCount: Int,
    showToolbarActions: Boolean,
    onSyncClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onResetReceiveHelper: () -> Unit,
    onViewByDate: () -> Unit,
) {
    CenterAlignedTopAppBar(
        colors =
            TopAppBarDefaults
                .centerAlignedTopAppBarColors()
                .copy(scrolledContainerColor = MaterialTheme.colorScheme.background),
        title = { },
        actions = {
            if (showToolbarActions) {
                IconButton(
                    onClick = onViewByDate,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .padding(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                    )
                }
                if (notUploadedCount > 0) {
                    Box(
                        modifier =
                            Modifier
                                .height(48.dp)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onSyncClicked),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.Center),
                        ) {
                            Text("$notUploadedCount", style = MaterialTheme.typography.labelMedium)
                            Icon(
                                imageVector = vectorResource(Res.drawable.sync_up),
                                contentDescription = null,
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onResetReceiveHelper,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .padding(4.dp),
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Outlined.Link else Icons.Outlined.LinkOff,
                        contentDescription = null,
                    )
                }
                IconButton(
                    onClick = onSearchClicked,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .padding(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(Res.string.search),
                    )
                }
                IconButton(
                    onClick = onSettingsClicked,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .padding(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(Res.string.settings),
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun ReconcileAllConfirmationDialog(
    showReconcileAllConfirmation: Boolean,
    onDismiss: () -> Unit,
    onReconcileAll: (uploadOnSuccess: Boolean) -> Unit,
) {
    var uploadOnSuccess by remember { mutableStateOf(true) }
    if (showReconcileAllConfirmation) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = stringResource(Res.string.reconcile_all))
            },
            text = {
                Column {
                    Text(stringResource(Res.string.reconcile_all_body))
                    Row(
                        modifier =
                            Modifier
                                .clickable(role = Role.Checkbox, onClick = { uploadOnSuccess = !uploadOnSuccess })
                                .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.reconcile_all_upload_on_success),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Checkbox(checked = uploadOnSuccess, onCheckedChange = null)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                        onReconcileAll(uploadOnSuccess)
                    },
                ) {
                    Text(stringResource(Res.string.proceed))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}
