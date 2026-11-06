package com.ramitsuri.notificationjournal.ai.diagnostics

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.ramitsuri.notificationjournal.ai.prompt.JournalPrompts
import com.ramitsuri.notificationjournal.ai.voice.SpeechEngine
import com.ramitsuri.notificationjournal.ai.voice.VoiceInputEvent
import com.ramitsuri.notificationjournal.ai.voice.VoiceInputManagerFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
    var selectedPromptTab by remember { mutableIntStateOf(0) }
    var listeningJob by remember { mutableStateOf<Job?>(null) }
    val isListening = listeningJob != null || state.isListeningToSpeech

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
            // Section 1: Model Capabilities & AICore Status
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Model Capabilities",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            StatusBadge(statusCode = state.modelInfo.statusCode, text = state.modelInfo.statusText)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Model: ${state.modelInfo.modelName.ifBlank { "Not loaded / Standard AICore" }}",
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

            // Section 2: Speech Recognition Engine
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Speech Recognition Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose which on-device engine processes voice input from the assistant mic button.",
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
                                                    FeatureStatus.AVAILABLE -> "Ready" to FeatureStatus.AVAILABLE
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

            // Section 2.5: Custom Prompt Rules
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Custom Prompt Rules",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add custom instructions for the LLM when extracting journal entries (e.g., abbreviations, custom formatting). One rule per line. These are included automatically in the prompt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.customPromptRules,
                            onValueChange = viewModel::onCustomPromptRulesChange,
                            placeholder = { Text("e.g.\nWhen I say workout, tag as #fitness\nReplace gym with fitness") },
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

            // Section 3: Copyable System Prompts
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Prompts (Copy for AICore App)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TabRow(selectedTabIndex = selectedPromptTab) {
                            Tab(
                                selected = selectedPromptTab == 0,
                                onClick = { selectedPromptTab = 0 },
                                text = { Text("JSON Prompt") },
                            )
                            Tab(
                                selected = selectedPromptTab == 1,
                                onClick = { selectedPromptTab = 1 },
                                text = { Text("Structured Prompt") },
                            )
                        }

                        val currentPrompt =
                            if (selectedPromptTab == 0) {
                                JournalPrompts.getAddEntryJsonPrompt(state.testInput, customRules = state.customPromptRules)
                            } else {
                                JournalPrompts.getAddEntryStructuredOutputPrompt(state.testInput, customRules = state.customPromptRules)
                            }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(currentPrompt))
                                Toast.makeText(context, "Prompt copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
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
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
            }

            // Section 3: Live Test & Output Inspector
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Live Test & Output Inspector",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

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
                                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = if (isListening) "Stop listening" else "Speak input",
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

                        state.structuredOutputAttempt?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Structured Output Log:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
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
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
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
                                    Text("Tag: ${intent.tag}", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "Target Date: ${intent.targetDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        "Target Time: ${intent.targetTime}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        "Reply: \"${intent.conversationalReply}\"",
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
