package com.ramitsuri.notificationjournal.ai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramitsuri.notificationjournal.core.model.voice.VoiceNote
import java.io.File

@Composable
fun VoiceNoteReviewDialog(
    voiceNote: VoiceNote,
    onDismiss: () -> Unit,
    viewModel: VoiceNoteReviewViewModel = viewModel(),
) {
    val context = LocalContext.current
    val isPlaying by viewModel.isPlayingFlow.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAudio()
        }
    }

    val audioFile = remember(voiceNote.filePath) { File(voiceNote.filePath) }

    VoiceNoteReviewContent(
        voiceNote = voiceNote,
        isPlaying = isPlaying,
        onPlayAudio = { viewModel.playAudio(audioFile) },
        onStopAudio = { viewModel.stopAudio() },
        onSave = { text ->
            viewModel.save(voiceNote, text, onDismiss)
        },
        onDelete = {
            viewModel.delete(voiceNote, onDismiss)
        },
        onRetry = {
            viewModel.retry(context, voiceNote, onDismiss)
        },
        onDismiss = onDismiss,
    )
}

@Composable
private fun VoiceNoteReviewContent(
    voiceNote: VoiceNote,
    isPlaying: Boolean,
    onPlayAudio: () -> Unit,
    onStopAudio: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    var textInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            onStopAudio()
            onDismiss()
        },
        title = {
            Text("Review Voice Note")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (!voiceNote.errorMessage.isNullOrBlank()) {
                    Text(
                        text = "Reason: ${voiceNote.errorMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Audio playback row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                onStopAudio()
                            } else {
                                onPlayAudio()
                            }
                        },
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (isPlaying) "Playing audio..." else "Listen to recording",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Entry text") },
                    placeholder = { Text("Type what you said...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSave(textInput)
                    }
                },
                enabled = textInput.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onDelete,
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }

                OutlinedButton(
                    onClick = onRetry,
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry")
                }
            }
        },
    )
}
