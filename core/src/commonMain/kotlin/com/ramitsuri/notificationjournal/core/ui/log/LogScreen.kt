package com.ramitsuri.notificationjournal.core.ui.log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ramitsuri.notificationjournal.core.log.LogData
import com.ramitsuri.notificationjournal.core.ui.components.Toolbar
import com.ramitsuri.notificationjournal.core.ui.fullBorder
import com.ramitsuri.notificationjournal.core.utils.formatTimeForLogs
import kotlinx.datetime.TimeZone
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.am
import notificationjournal.core.generated.resources.pm
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    logs: List<LogData>,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    onBackClick: () -> Unit,
    onClearLogsClick: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
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
            Toolbar(
                onBackClick = onBackClick,
                scrollBehavior = null,
                actions = {
                    Row {
                        IconButton(onClick = onClearLogsClick) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteForever,
                                contentDescription = null,
                            )
                        }
                    }
                },
            )

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(logs) { logData ->
                    LogItem(logData, timeZone)
                }
            }
        }
    }
}

@Composable
private fun LogItem(
    logData: LogData,
    timeZone: TimeZone,
) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .fullBorder(1.dp, MaterialTheme.colorScheme.outline, 16.dp)
                .padding(16.dp),
    ) {
        var showStackTrace by remember { mutableStateOf(false) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        style = MaterialTheme.typography.labelSmall,
                        text =
                            formatTimeForLogs(
                                toFormat = logData.time,
                                timeZone = timeZone,
                                amString = stringResource(Res.string.am),
                                pmString = stringResource(Res.string.pm),
                            ),
                    )
                    if (logData.tag.isNotEmpty()) {
                        Text(
                            text = "\u2022",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        Text(
                            text = logData.tag,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Text(
                    style = MaterialTheme.typography.bodyMedium,
                    text = logData.message,
                )
            }
            if (logData.errorMessage != null || logData.stackTrace != null) {
                IconButton(onClick = { showStackTrace = !showStackTrace }) {
                    Icon(
                        imageVector =
                            if (showStackTrace) {
                                Icons.Default.ArrowDropUp
                            } else {
                                Icons.Default.ArrowDropDown
                            },
                        contentDescription = null,
                    )
                }
            }
        }
        AnimatedVisibility(showStackTrace) {
            Column {
                logData.errorMessage?.let {
                    Text(
                        style = MaterialTheme.typography.bodySmall,
                        text = it,
                    )
                }
                logData.stackTrace?.let {
                    Text(
                        style = MaterialTheme.typography.bodySmall,
                        text = it,
                    )
                }
            }
        }
    }
}
