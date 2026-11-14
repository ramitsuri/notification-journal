package com.ramitsuri.notificationjournal.ai.diagnostics

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.genai.common.FeatureStatus
import com.ramitsuri.notificationjournal.ai.extractor.CustomModelDownloadState
import com.ramitsuri.notificationjournal.ai.prompt.JournalPrompts
import com.ramitsuri.notificationjournal.ai.voice.SpeechEngine
import com.ramitsuri.notificationjournal.ai.voice.VoiceInputEvent
import com.ramitsuri.notificationjournal.ai.voice.VoiceInputManagerFactory
import com.ramitsuri.notificationjournal.core.log.LogData
import com.ramitsuri.notificationjournal.core.utils.formatTimeForLogs
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDiagnosticsScreen(
    viewModel: AiDiagnosticsViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var listeningJob by remember { mutableStateOf<Job?>(null) }
    val isListening = listeningJob != null || state.isListeningToSpeech

    var isLiveTestExpanded by rememberSaveable { mutableStateOf(true) }
    var isFallbackLogsExpanded by rememberSaveable { mutableStateOf(false) }
    var isModelCapabilitiesExpanded by rememberSaveable { mutableStateOf(false) }
    var isCustomModelExpanded by rememberSaveable { mutableStateOf(false) }
    var isSpeechEngineExpanded by rememberSaveable { mutableStateOf(false) }
    var isCustomRulesExpanded by rememberSaveable { mutableStateOf(false) }
    var isPromptsExpanded by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            listeningJob?.cancel()
            listeningJob = null
        }
    }

    val startListening = {
        listeningJob?.cancel()
        val engine = state.selectedSpeechEngine
        val manager = VoiceInputManagerFactory.create(context, engine)
        viewModel.onSpeechListeningStarted()
        val startTime = System.currentTimeMillis()
        listeningJob =
            coroutineScope.launch {
                manager.startListening().collect { event ->
                    when (event) {
                        VoiceInputEvent.SpeechStarted -> {}
                        is VoiceInputEvent.PartialResult -> {
                            viewModel.onTestInputChange(event.text)
                        }

                        is VoiceInputEvent.FinalResult -> {
                            val duration = System.currentTimeMillis() - startTime
                            viewModel.onSpeechFinalResult(
                                text = event.text,
                                engineName = engine.displayName,
                                durationMs = duration,
                            )
                            listeningJob = null
                        }

                        is VoiceInputEvent.Error -> {
                            viewModel.onSpeechError(event.message)
                            listeningJob = null
                        }
                    }
                }
            }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                startListening()
            } else {
                Toast.makeText(context, "Microphone permission required for speech input.", Toast.LENGTH_SHORT).show()
            }
        }

    val requestListeningWithPermission = {
        val hasPermission =
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startListening()
        } else {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    val stopListening = {
        listeningJob?.cancel()
        listeningJob = null
        val currentInput = state.testInput
        if (currentInput.isNotBlank()) {
            viewModel.onSpeechFinalResult(
                text = currentInput,
                engineName = state.selectedSpeechEngine.displayName,
                durationMs = 0L,
            )
        } else {
            viewModel.onSpeechListeningStopped()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gemini Nano Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refreshModelInfo) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        modifier =
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .displayCutoutPadding(),
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Section 1: Live Test & Output Inspector
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { isLiveTestExpanded = !isLiveTestExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Live Test & Output Inspector",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Icon(
                                imageVector =
                                    if (isLiveTestExpanded) {
                                        Icons.Default.ArrowDropUp
                                    } else {
                                        Icons.Default.ArrowDropDown
                                    },
                                contentDescription = if (isLiveTestExpanded) "Collapse" else "Expand",
                            )
                        }

                        AnimatedVisibility(visible = isLiveTestExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = state.testInput,
                                    onValueChange = viewModel::onTestInputChange,
                                    label = { Text("Test Input") },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                if (isListening) {
                                                    stopListening()
                                                } else {
                                                    requestListeningWithPermission()
                                                }
                                            },
                                        ) {
                                            Icon(
                                                imageVector =
                                                    if (isListening) {
                                                        Icons.Default.MicOff
                                                    } else {
                                                        Icons.Default.Mic
                                                    },
                                                contentDescription =
                                                    if (isListening) {
                                                        "Stop listening"
                                                    } else {
                                                        "Speak input"
                                                    },
                                                tint =
                                                    if (isListening) {
                                                        MaterialTheme.colorScheme.error
                                                    } else {
                                                        MaterialTheme.colorScheme.primary
                                                    },
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "Auto-run extraction on speech completion",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Switch(
                                        checked = state.autoRunExtractionAfterSpeech,
                                        onCheckedChange = viewModel::toggleAutoRunExtraction,
                                    )
                                }

                                if (isListening) {
                                    Text(
                                        text = "Listening via ${state.selectedSpeechEngine.displayName}...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = viewModel::runLiveTest,
                                    enabled = !state.isTesting,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    if (state.isTesting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Running Inference...")
                                    } else {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Run Test Extraction")
                                    }
                                }

                                if (state.isTesting) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                                }

                                state.engineUsed?.let { engine ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "Final Engine Used:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = engine,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }

                                state.rawModelResponse?.let {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Raw Text from Gemini Nano:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            text = it,
                                            style =
                                                MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                ),
                                            modifier = Modifier.padding(8.dp),
                                        )
                                    }
                                }

                                state.parsedIntent?.let { intent ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Parsed Result (AddEntryIntent):",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                "Entry Text: \"${intent.entryText}\"",
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                            Text(
                                                "Target Date: ${intent.targetDate}",
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                            Text(
                                                "Target Time: ${intent.targetTime}",
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }

                                if (state.executionLog.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Execution Trace:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    state.executionLog.forEach { logLine ->
                                        Text("• $logLine", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Rule-Based Fallbacks
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { isFallbackLogsExpanded = !isFallbackLogsExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Rule-Based Fallbacks",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (state.fallbackLogs.isNotEmpty()) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.error,
                                                    shape = RoundedCornerShape(12.dp),
                                                ).padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = state.fallbackLogs.size.toString(),
                                            color = MaterialTheme.colorScheme.onError,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector =
                                    if (isFallbackLogsExpanded) {
                                        Icons.Default.ArrowDropUp
                                    } else {
                                        Icons.Default.ArrowDropDown
                                    },
                                contentDescription = if (isFallbackLogsExpanded) "Collapse" else "Expand",
                            )
                        }

                        AnimatedVisibility(visible = isFallbackLogsExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text =
                                        "Logs recorded whenever Gemini Nano is unavailable, fails generation, " +
                                            "or produces unparseable output, falling back to rule-based extraction.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                if (state.fallbackLogs.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            text = "No rule-based fallbacks recorded in this session.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(12.dp),
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                    ) {
                                        OutlinedButton(
                                            onClick = viewModel::clearFallbackLogs,
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Clear Fallback Logs")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        state.fallbackLogs.forEach { logData ->
                                            FallbackLogItem(
                                                logData = logData,
                                                onCopy = { textToCopy ->
                                                    clipboardManager.setText(AnnotatedString(textToCopy))
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            "Log copied to clipboard",
                                                            Toast.LENGTH_SHORT,
                                                        ).show()
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Model Capabilities & AICore Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { isModelCapabilitiesExpanded = !isModelCapabilitiesExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Model Capabilities",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                StatusBadge(statusCode = state.modelInfo.statusCode, text = state.modelInfo.statusText)
                                Icon(
                                    imageVector =
                                        if (isModelCapabilitiesExpanded) {
                                            Icons.Default.ArrowDropUp
                                        } else {
                                            Icons.Default.ArrowDropDown
                                        },
                                    contentDescription = if (isModelCapabilitiesExpanded) "Collapse" else "Expand",
                                )
                            }
                        }

                        AnimatedVisibility(visible = isModelCapabilitiesExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Model: ${state.modelInfo.modelName.ifBlank {
                                        "Not loaded / Standard AICore"
                                    }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = "Token Limit: ${state.modelInfo.tokenLimit}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Feature Support:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FeatureRow("Structured Output (Constrained)", state.modelInfo.structuredOutputSupported)
                                FeatureRow("System Prompt", state.modelInfo.systemPromptSupported)
                                FeatureRow("Thinking Mode", state.modelInfo.thinkingModeSupported)
                                FeatureRow("Prefix Caching", state.modelInfo.prefixCachingSupported)
                                FeatureRow("Function Calling", state.modelInfo.functionCallingSupported)

                                if (state.modelInfo.statusCode == FeatureStatus.DOWNLOADABLE ||
                                    state.modelInfo.statusCode == FeatureStatus.DOWNLOADING
                                ) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = viewModel::triggerModelDownload,
                                        enabled = !state.isDownloading,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (state.isDownloading) "Downloading..." else "Download Model Weights")
                                    }
                                    state.downloadStatus?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(top = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Custom On-Device Model (LiteRT-LM)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { isCustomModelExpanded = !isCustomModelExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "Custom Model (LiteRT-LM)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text =
                                        if (state.customModelInfo.isPresent) {
                                            state.customModelInfo.fileName ?: "Detected"
                                        } else {
                                            "Not detected (using AICore / Rule fallback)"
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        if (state.customModelInfo.isPresent) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                StatusBadge(
                                    statusCode =
                                        if (state.customModelInfo.isPresent) {
                                            FeatureStatus.AVAILABLE
                                        } else {
                                            FeatureStatus.UNAVAILABLE
                                        },
                                    text = if (state.customModelInfo.isPresent) "LOADED" else "NOT FOUND",
                                )
                                Icon(
                                    imageVector =
                                        if (isCustomModelExpanded) {
                                            Icons.Default.ArrowDropUp
                                        } else {
                                            Icons.Default.ArrowDropDown
                                        },
                                    contentDescription = if (isCustomModelExpanded) "Collapse" else "Expand",
                                )
                            }
                        }

                        AnimatedVisibility(visible = isCustomModelExpanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                if (state.customModelInfo.isPresent) {
                                    Text(
                                        text = "File: ${state.customModelInfo.fileName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "Size: ${state.customModelInfo.fileSizeFormatted}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        text = "Path: ${state.customModelInfo.filePath}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text =
                                            "✓ This model is used for intent extraction with unrestricted background " +
                                                "execution support (no AICore foreground restriction).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedButton(
                                        onClick = viewModel::deleteCustomModel,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Delete Model")
                                    }
                                } else {
                                    val customModel = state.supportedCustomModel
                                    Text(
                                        text = "No custom .litertlm model found on device storage.",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text =
                                            "Downloads ${customModel.displayName} " +
                                                "for on-device intent extraction without AICore background restrictions.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    when (val downloadState = state.customModelDownloadState) {
                                        is CustomModelDownloadState.Downloading -> {
                                            val downloadedMb = downloadState.bytesDownloaded / (1024 * 1024)
                                            val totalMb = downloadState.totalBytes / (1024 * 1024)
                                            val progressText =
                                                if (totalMb > 0) {
                                                    "Downloading: $downloadedMb MB / $totalMb MB " +
                                                        "(${downloadState.progressPercent}%)"
                                                } else {
                                                    "Downloading: $downloadedMb MB..."
                                                }
                                            Text(
                                                text = progressText,
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            LinearProgressIndicator(
                                                progress = { downloadState.progressPercent / 100f },
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedButton(
                                                onClick = viewModel::cancelCustomModelDownload,
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Text("Cancel Download")
                                            }
                                        }
                                        is CustomModelDownloadState.Failed -> {
                                            Text(
                                                text = "Download error: ${downloadState.error}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = { viewModel.startCustomModelDownload() },
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Retry Download ${customModel.displayName}")
                                            }
                                        }
                                        else -> {
                                            Button(
                                                onClick = { viewModel.startCustomModelDownload() },
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Download ${customModel.displayName}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Speech Recognition Engine
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { isSpeechEngineExpanded = !isSpeechEngineExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "Speech Recognition Engine",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (!isSpeechEngineExpanded) {
                                    Text(
                                        text = state.selectedSpeechEngine.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Icon(
                                imageVector =
                                    if (isSpeechEngineExpanded) {
                                        Icons.Default.ArrowDropUp
                                    } else {
                                        Icons.Default.ArrowDropDown
                                    },
                                contentDescription = if (isSpeechEngineExpanded) "Collapse" else "Expand",
                            )
                        }

                        AnimatedVisibility(visible = isSpeechEngineExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text =
                                        "Choose which on-device engine processes voice input " +
                                            "from the assistant mic button.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SpeechEngine.entries.forEach { engine ->
                                    val isSelected = state.selectedSpeechEngine == engine
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .selectable(
                                                    selected = isSelected,
                                                    role = Role.RadioButton,
                                                    onClick = { viewModel.onSpeechEngineSelected(engine) },
                                                ).padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = null,
                                            modifier = Modifier.padding(top = 2.dp),
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Text(
                                                    text = engine.displayName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                                if (engine == SpeechEngine.MLKIT_GENAI) {
                                                    val (statusText, code) =
                                                        when (state.mlKitSpeechStatus) {
                                                            FeatureStatus.AVAILABLE ->
                                                                "Ready" to
                                                                    FeatureStatus.AVAILABLE
                                                            FeatureStatus.DOWNLOADABLE ->
                                                                "Downloadable" to
                                                                    FeatureStatus.DOWNLOADABLE
                                                            FeatureStatus.DOWNLOADING ->
                                                                "Downloading" to
                                                                    FeatureStatus.DOWNLOADING
                                                            FeatureStatus.UNAVAILABLE ->
                                                                "Unavailable" to
                                                                    FeatureStatus.UNAVAILABLE
                                                            else -> "Checking..." to -1
                                                        }
                                                    StatusBadge(statusCode = code, text = statusText)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = engine.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )

                                            if (engine == SpeechEngine.MLKIT_GENAI &&
                                                (
                                                    state.mlKitSpeechStatus == FeatureStatus.DOWNLOADABLE ||
                                                        state.mlKitSpeechStatus == FeatureStatus.DOWNLOADING
                                                )
                                            ) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Button(
                                                    onClick = viewModel::triggerSpeechModelDownload,
                                                    enabled = !state.isDownloadingSpeech,
                                                    modifier = Modifier.fillMaxWidth(),
                                                ) {
                                                    Icon(Icons.Default.Download, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        if (state.isDownloadingSpeech) {
                                                            "Downloading Speech Model..."
                                                        } else {
                                                            "Download Speech Model"
                                                        },
                                                    )
                                                }
                                                state.speechDownloadStatus?.let {
                                                    Text(
                                                        text = it,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(top = 4.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Custom Prompt Rules
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { isCustomRulesExpanded = !isCustomRulesExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Custom Prompt Rules",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Icon(
                                imageVector =
                                    if (isCustomRulesExpanded) {
                                        Icons.Default.ArrowDropUp
                                    } else {
                                        Icons.Default.ArrowDropDown
                                    },
                                contentDescription = if (isCustomRulesExpanded) "Collapse" else "Expand",
                            )
                        }

                        AnimatedVisibility(visible = isCustomRulesExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text =
                                        "Add custom instructions for the LLM when extracting journal entries " +
                                            "(e.g., abbreviations, custom formatting). One rule per line. " +
                                            "These are included automatically in the prompt.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = state.customPromptRules,
                                    onValueChange = viewModel::onCustomPromptRulesChange,
                                    placeholder = {
                                        Text(
                                            "e.g.\nWhen I say workout, tag as #fitness\nReplace gym with fitness",
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 6,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (state.customRulesSavedMessage) {
                                        Text(
                                            text = "Rules saved!",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }
                                    Button(onClick = viewModel::saveCustomPromptRules) {
                                        Text("Save Rules")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 5: Copyable System Prompts
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { isPromptsExpanded = !isPromptsExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Prompts (Copy for AICore App)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Icon(
                                imageVector =
                                    if (isPromptsExpanded) {
                                        Icons.Default.ArrowDropUp
                                    } else {
                                        Icons.Default.ArrowDropDown
                                    },
                                contentDescription = if (isPromptsExpanded) "Collapse" else "Expand",
                            )
                        }

                        AnimatedVisibility(visible = isPromptsExpanded) {
                            Column {
                                val currentPrompt =
                                    JournalPrompts.getAddEntryPrompt(
                                        state.testInput,
                                        customRules = state.customPromptRules,
                                    )

                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(currentPrompt))
                                        Toast
                                            .makeText(
                                                context,
                                                "Prompt copied to clipboard!",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Copy Prompt to Clipboard")
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = currentPrompt,
                                        style =
                                            MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                            ),
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    statusCode: Int,
    text: String,
) {
    val (bgColor, textColor) =
        when (statusCode) {
            FeatureStatus.AVAILABLE -> Color(0xFF2E7D32) to Color.White
            FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> Color(0xFFE65100) to Color.White
            else -> Color(0xFFC62828) to Color.White
        }

    Box(
        modifier =
            Modifier
                .background(bgColor, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FeatureRow(
    name: String,
    supported: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = name, style = MaterialTheme.typography.bodySmall)
        if (supported) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Supported", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Not supported",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun FallbackLogItem(
    logData: LogData,
    onCopy: (String) -> Unit,
) {
    var showStackTrace by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        formatTimeForLogs(
                            toFormat = logData.time,
                            timeZone = TimeZone.currentSystemDefault(),
                            amString = "AM",
                            pmString = "PM",
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val fullLog =
                                buildString {
                                    append(logData.message)
                                    if (!logData.errorMessage.isNullOrBlank()) {
                                        append("\nError: ").append(logData.errorMessage)
                                    }
                                    if (!logData.stackTrace.isNullOrBlank()) {
                                        append("\nStacktrace:\n").append(logData.stackTrace)
                                    }
                                }
                            onCopy(fullLog)
                        },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy log",
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    if (logData.errorMessage != null || logData.stackTrace != null) {
                        IconButton(
                            onClick = { showStackTrace = !showStackTrace },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector =
                                    if (showStackTrace) {
                                        Icons.Default.ArrowDropUp
                                    } else {
                                        Icons.Default.ArrowDropDown
                                    },
                                contentDescription = if (showStackTrace) "Hide details" else "Show details",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = logData.message,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )

            AnimatedVisibility(visible = showStackTrace) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    logData.errorMessage?.let {
                        Text(
                            text = "Error: $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    logData.stackTrace?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
