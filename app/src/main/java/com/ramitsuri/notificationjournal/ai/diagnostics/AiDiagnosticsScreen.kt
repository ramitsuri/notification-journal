package com.ramitsuri.notificationjournal.ai.diagnostics

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.genai.common.FeatureStatus
import com.ramitsuri.notificationjournal.ai.prompt.JournalPrompts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDiagnosticsScreen(
    viewModel: AiDiagnosticsViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var selectedPromptTab by remember { mutableIntStateOf(0) }

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

            // Section 2: Copyable System Prompts
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
                                JournalPrompts.getJsonPrompt(state.testInput)
                            } else {
                                JournalPrompts.getStructuredOutputPrompt(state.testInput)
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
                            modifier = Modifier.fillMaxWidth(),
                        )

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
                                text = "Parsed Result (JournalIntent):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Action: ${intent.action}", style = MaterialTheme.typography.bodySmall)
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
                                        "Search Keyword: ${intent.searchKeyword}",
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
