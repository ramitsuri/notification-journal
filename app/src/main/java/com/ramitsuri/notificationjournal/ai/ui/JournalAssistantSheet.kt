package com.ramitsuri.notificationjournal.ai.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramitsuri.notificationjournal.ai.AssistantUiState
import com.ramitsuri.notificationjournal.ai.JournalAssistantViewModel
import com.ramitsuri.notificationjournal.ai.voice.VoiceInputManager

@Composable
fun JournalAssistantSheet(
    viewModel: JournalAssistantViewModel = viewModel(),
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
            )
        }

    DisposableEffect(voiceInputManager) {
        onDispose {
            voiceInputManager.stopListening()
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

    JournalAssistantSheet(
        viewModel = viewModel,
        onStartVoice = startVoice,
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalAssistantSheet(
    viewModel: JournalAssistantViewModel,
    onStartVoice: () -> Unit,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var textInput by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                text = "Journal Assistant",
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (val current = state) {
                is AssistantUiState.Idle -> {
                    Text("Ask to search past entries or dictate a new entry.")
                }
                is AssistantUiState.Listening -> {
                    Text("Listening...", color = MaterialTheme.colorScheme.primary)
                }
                is AssistantUiState.Processing -> {
                    Text("Processing: \"${current.transcription}\"")
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
                is AssistantUiState.Success -> {
                    Text(current.message, style = MaterialTheme.typography.bodyLarge)
                    if (current.engine.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Processed via ${current.engine}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                    if (current.matchedEntries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(current.matchedEntries) { entry ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(entry.text, style = MaterialTheme.typography.bodyMedium)
                                        Text(entry.entryTime.toString(), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
                is AssistantUiState.Error -> {
                    Text(current.error, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Type or tap mic...") },
                    modifier = Modifier.weight(1f),
                )

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.processQuery(textInput)
                            textInput = ""
                        }
                    },
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Text")
                }

                IconButton(onClick = onStartVoice) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice Input")
                }
            }
        }
    }
}
