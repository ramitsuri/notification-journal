package com.ramitsuri.notificationjournal.ai.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramitsuri.notificationjournal.ai.AssistantUiState
import com.ramitsuri.notificationjournal.ai.JournalAssistantViewModel

@Composable
fun JournalAssistantSheet(
    viewModel: JournalAssistantViewModel = viewModel(),
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    val stopRecording = {
        viewModel.discardRecording()
    }

    DisposableEffect(Unit) {
        onDispose {
            stopRecording()
        }
    }

    val startRecording = {
        viewModel.startRecording(context)
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                startRecording()
            }
        }

    val requestPermissionAndStart = {
        val hasPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startRecording()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) {
        requestPermissionAndStart()
    }

    JournalAssistantContent(
        viewModel = viewModel,
        onStartRecording = requestPermissionAndStart,
        onNextEntry = { viewModel.nextEntry(context) },
        onCommit = { viewModel.commitRecording(context, onDismiss) },
        onDiscard = {
            stopRecording()
            onDismiss()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalAssistantContent(
    viewModel: JournalAssistantViewModel,
    onStartRecording: () -> Unit,
    onNextEntry: () -> Unit,
    onCommit: () -> Unit,
    onDiscard: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val amplitude by viewModel.amplitudeFlow.collectAsState()
    var showConfirmDiscardDialog by remember { mutableStateOf(false) }

    val isRecording = state is AssistantUiState.Recording

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { targetValue ->
                if (targetValue == SheetValue.Hidden && isRecording) {
                    showConfirmDiscardDialog = true
                    false
                } else {
                    true
                }
            },
        )

    val handleDismissRequest = {
        if (isRecording) {
            showConfirmDiscardDialog = true
        } else {
            onDiscard()
        }
    }

    if (showConfirmDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDiscardDialog = false },
            title = { Text("Discard recording?") },
            text = { Text("Whatever was recorded in this session will be lost. Are you sure?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDiscardDialog = false
                        onDiscard()
                    },
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDiscardDialog = false }) {
                    Text("Keep Recording")
                }
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = handleDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val current = state) {
                is AssistantUiState.Idle -> {
                    Box(
                        modifier = Modifier.size(180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(
                            onClick = onStartRecording,
                            modifier =
                                Modifier
                                    .size(72.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape,
                                    ),
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Start Recording",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tap microphone to record",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is AssistantUiState.Recording -> {
                    // Pulsating live amplitude concentric rings
                    val outerPulseSize by animateDpAsState(
                        targetValue = 100.dp + (60.dp * amplitude.coerceIn(0f, 1f)),
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                        label = "OuterPulseSize",
                    )
                    val middlePulseSize by animateDpAsState(
                        targetValue = 84.dp + (34.dp * amplitude.coerceIn(0f, 1f)),
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                        label = "MiddlePulseSize",
                    )

                    Box(
                        modifier = Modifier.size(180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Outer pulse ring
                        Box(
                            modifier =
                                Modifier
                                    .size(outerPulseSize)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        shape = CircleShape,
                                    ),
                        )
                        // Middle pulse ring
                        Box(
                            modifier =
                                Modifier
                                    .size(middlePulseSize)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        shape = CircleShape,
                                    ),
                        )
                        // Inner mic button
                        Box(
                            modifier =
                                Modifier
                                    .size(68.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Recording active",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val minutes = current.durationSeconds / 60
                    val seconds = current.durationSeconds % 60
                    val timeFormatted = "%02d:%02d".format(minutes, seconds)

                    Text(
                        text = "Entry ${current.segmentIndex} • $timeFormatted",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    AnimatedVisibility(
                        visible = current.silentMicWarning,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier =
                                    Modifier
                                        .background(
                                            MaterialTheme.colorScheme.errorContainer,
                                            shape = RoundedCornerShape(8.dp),
                                        ).padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Microphone is not receiving audio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Action buttons: Discard, Next Entry, Done
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                if (current.durationSeconds > 1 || current.segmentIndex > 1) {
                                    showConfirmDiscardDialog = true
                                } else {
                                    onDiscard()
                                }
                            },
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Discard", color = MaterialTheme.colorScheme.error)
                        }

                        FilledTonalButton(onClick = onNextEntry) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Next Entry")
                        }

                        Button(
                            onClick = onCommit,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Done")
                        }
                    }
                }

                is AssistantUiState.Error -> {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = current.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    ) {
                        Button(onClick = onStartRecording) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Try Again")
                        }
                        OutlinedButton(onClick = onDiscard) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}
