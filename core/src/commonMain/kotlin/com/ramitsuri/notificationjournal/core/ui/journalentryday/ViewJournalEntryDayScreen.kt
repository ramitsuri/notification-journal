package com.ramitsuri.notificationjournal.core.ui.journalentryday

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.ramitsuri.notificationjournal.core.ui.components.Date
import com.ramitsuri.notificationjournal.core.ui.components.DayGroupAction
import com.ramitsuri.notificationjournal.core.ui.components.JournalEntryDay
import com.ramitsuri.notificationjournal.core.ui.components.JournalEntryDayConfig
import com.ramitsuri.notificationjournal.core.ui.components.Toolbar
import kotlinx.datetime.LocalDate
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.no_items
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewJournalEntryDayScreen(
    state: ViewState,
    onBackClick: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onAction: (DayGroupAction) -> Unit,
    onContentCopied: () -> Unit,
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    LaunchedEffect(state.contentForCopy) {
        if (state.contentForCopy.isNotEmpty()) {
            clipboardManager.setText(AnnotatedString(state.contentForCopy))
            onContentCopied()
        }
    }
    var showDatePicker by remember { mutableStateOf(false) }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .focusable(),
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
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior,
                actions = {
                    Row {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                            )
                        }
                    }
                },
            )

            if (state.dayGroup == null) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, bottom = 64.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(Res.string.no_items),
                        style = MaterialTheme.typography.displaySmall,
                    )
                }
            } else {
                JournalEntryDay(
                    dayGroup = state.dayGroup,
                    tags = listOf(),
                    conflictCount = 0,
                    conflicts = listOf(),
                    scrollConnection = scrollBehavior.nestedScrollConnection,
                    showEmptyTags = false,
                    showConflictDiffInline = false,
                    onAction = onAction,
                    config = JournalEntryDayConfig.allDisabled,
                )
            }
        }
    }
    if (showDatePicker) {
        Date(
            selectedDate = state.selectedDate,
            onDismiss = { showDatePicker = false },
            onResetDate = { },
            onResetDateToToday = null,
            onDateSelected = {
                showDatePicker = false
                onDateSelected(it)
            },
        )
    }
}
