package com.ramitsuri.notificationjournal.ai.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramitsuri.notificationjournal.ai.AssistantUiState
import com.ramitsuri.notificationjournal.ai.JournalAssistantViewModel
import com.ramitsuri.notificationjournal.ai.voice.VoiceInputManager

@Composable
fun JournalAssistantSheet(
    viewModel: JournalAssistantViewModel = viewModel(factory = JournalAssistantViewModel.factory()),
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val voiceInputManager =
        remember(context, viewModel) {
            VoiceInputManager(
                context = context,
                onResult = { text ->
                    viewModel.processQuery(text)
                },
                onError = { error ->
                    viewModel.onError(error)
                },
                onSpeechStarted = {
                    viewModel.onSpeechStarted()
                },
                onPartialResult = { partial ->
                    viewModel.onPartialTranscription(partial)
                },
            )
        }

    val stopVoice = {
        voiceInputManager.stopListening()
        viewModel.resetState()
    }

    DisposableEffect(voiceInputManager) {
        onDispose {
            stopVoice()
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                viewModel.onListeningStarted()
                voiceInputManager.startListening()
            } else {
                viewModel.onError("Microphone permission required for voice input.")
            }
        }

    val startVoice = {
        val hasPermission =
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.onListeningStarted()
            voiceInputManager.startListening()
        } else {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    // Immediately start listening to mic audio when the sheet opens on long press
    LaunchedEffect(Unit) {
        startVoice()
    }

    JournalAssistantContent(
        viewModel = viewModel,
        onStartVoice = startVoice,
        onStopVoice = stopVoice,
        onDismiss = {
            stopVoice()
            onDismiss()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalAssistantContent(
    viewModel: JournalAssistantViewModel,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }

    val hasInput =
        when (val current = state) {
            is AssistantUiState.Listening -> current.hasSpeechStarted || current.partialText.isNotBlank()
            is AssistantUiState.Processing -> true
            else -> false
        }

    val sheetState =
        rememberModalBottomSheetState(
            confirmValueChange = { targetValue ->
                if (targetValue == SheetValue.Hidden && hasInput) {
                    showConfirmDialog = true
                    false
                } else {
                    true
                }
            },
        )

    val handleDismissRequest = {
        if (hasInput) {
            showConfirmDialog = true
        } else {
            onDismiss()
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Discard recording?") },
            text = { Text("Whatever was said will be lost. Are you sure?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onDismiss()
                    },
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Keep Speaking")
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
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val current = state) {
                is AssistantUiState.Idle -> {
                    IconButton(
                        onClick = onStartVoice,
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
                            contentDescription = "Start Listening",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tap microphone to speak",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is AssistantUiState.Listening -> {
                    IconButton(
                        onClick = onStopVoice,
                        modifier =
                            Modifier
                                .size(72.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                ),
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Stop Listening",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Listening...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (current.partialText.isNotBlank()) {
                        Text(
                            text = "\"${current.partialText}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    } else {
                        Text(
                            text = "Speak your entry or search query",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is AssistantUiState.Processing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 4.dp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Processing on device...",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"${current.transcription}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                is AssistantUiState.Success -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = current.message,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    if (current.engine.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Processed via ${current.engine}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }

                    if (current.matchedEntries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(current.matchedEntries) { entry ->
                                Card(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(entry.text, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            entry.entryTime.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    ) {
                        OutlinedButton(onClick = onStartVoice) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Speak Again")
                        }
                        Button(onClick = onDismiss) {
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
                        Button(onClick = onStartVoice) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Try Again")
                        }
                        OutlinedButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}
