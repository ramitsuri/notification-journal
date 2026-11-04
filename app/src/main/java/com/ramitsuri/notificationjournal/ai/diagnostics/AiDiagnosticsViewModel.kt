package com.ramitsuri.notificationjournal.ai.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Feature
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateTypedContentRequest
import com.ramitsuri.notificationjournal.ai.extractor.NanoJournalIntentExtractor
import com.ramitsuri.notificationjournal.ai.model.JournalIntent
import com.ramitsuri.notificationjournal.ai.prompt.JournalPrompts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AiModelInfo(
    val modelName: String = "",
    val statusCode: Int = -1,
    val statusText: String = "Checking...",
    val tokenLimit: Int = 0,
    val structuredOutputSupported: Boolean = false,
    val systemPromptSupported: Boolean = false,
    val thinkingModeSupported: Boolean = false,
    val prefixCachingSupported: Boolean = false,
    val functionCallingSupported: Boolean = false,
    val rawFeatures: List<Int> = emptyList(),
)

data class DiagnosticsUiState(
    val modelInfo: AiModelInfo = AiModelInfo(),
    val isRefreshing: Boolean = false,
    val testInput: String = "add a new entry that I slept well",
    val isTesting: Boolean = false,
    val structuredOutputAttempt: String? = null,
    val rawModelResponse: String? = null,
    val parsedIntent: JournalIntent? = null,
    val engineUsed: String? = null,
    val executionLog: List<String> = emptyList(),
    val isDownloading: Boolean = false,
    val downloadStatus: String? = null,
)

class AiDiagnosticsViewModel(
    private val generativeModel: GenerativeModel = Generation.getClient(),
    private val nanoExtractor: NanoJournalIntentExtractor = NanoJournalIntentExtractor(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshModelInfo()
    }

    fun onTestInputChange(input: String) {
        _uiState.update { it.copy(testInput = input) }
    }

    fun refreshModelInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val info =
                withContext(Dispatchers.IO) {
                    val status =
                        try {
                            generativeModel.checkStatus()
                        } catch (e: Exception) {
                            -1
                        }

                    val statusText =
                        when (status) {
                            FeatureStatus.AVAILABLE -> "AVAILABLE (Ready for inference)"
                            FeatureStatus.DOWNLOADABLE -> "DOWNLOADABLE (Model weights needed)"
                            FeatureStatus.DOWNLOADING -> "DOWNLOADING..."
                            FeatureStatus.UNAVAILABLE -> "UNAVAILABLE"
                            else -> "UNKNOWN / ERROR"
                        }

                    val name =
                        try {
                            generativeModel.getBaseModelName()
                        } catch (e: Exception) {
                            "Unknown (AICore)"
                        }

                    val tokenLimit =
                        try {
                            generativeModel.getTokenLimit()
                        } catch (e: Exception) {
                            0
                        }

                    val features =
                        try {
                            generativeModel.getAvailableFeatures()
                        } catch (e: Exception) {
                            emptyList()
                        }

                    AiModelInfo(
                        modelName = name,
                        statusCode = status,
                        statusText = statusText,
                        tokenLimit = tokenLimit,
                        structuredOutputSupported = features.contains(Feature.STRUCTURED_OUTPUT),
                        systemPromptSupported = features.contains(Feature.SYSTEM_PROMPT),
                        thinkingModeSupported = features.contains(Feature.THINKING_MODE),
                        prefixCachingSupported = features.contains(Feature.PREFIX_CACHING),
                        functionCallingSupported = features.contains(Feature.FUNCTION_CALLING),
                        rawFeatures = features,
                    )
                }

            _uiState.update { it.copy(modelInfo = info, isRefreshing = false) }
        }
    }

    fun triggerModelDownload() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, downloadStatus = "Initiating download...") }
            try {
                generativeModel.download().collect { status ->
                    _uiState.update { it.copy(downloadStatus = status.toString()) }
                }
                refreshModelInfo()
            } catch (e: Exception) {
                _uiState.update { it.copy(downloadStatus = "Download error: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isDownloading = false) }
            }
        }
    }

    fun runLiveTest() {
        val input = _uiState.value.testInput
        if (input.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTesting = true,
                    structuredOutputAttempt = null,
                    rawModelResponse = null,
                    parsedIntent = null,
                    engineUsed = null,
                    executionLog = emptyList(),
                )
            }

            val logs = mutableListOf<String>()
            var structuredResult: String? = null
            var rawText: String? = null

            withContext(Dispatchers.IO) {
                // Step 1: Check status
                val status =
                    try {
                        generativeModel.checkStatus()
                    } catch (e: Exception) {
                        logs.add("checkStatus() threw: ${e.message}")
                        FeatureStatus.UNAVAILABLE
                    }
                logs.add("Status: $status")

                if (status == FeatureStatus.AVAILABLE) {
                    val features =
                        try {
                            generativeModel.getAvailableFeatures()
                        } catch (e: Exception) {
                            emptyList()
                        }
                    val hasStructured = features.contains(Feature.STRUCTURED_OUTPUT)
                    logs.add("Features: $features (STRUCTURED_OUTPUT=$hasStructured)")

                    // Attempt Tier 1: Structured Output
                    if (hasStructured) {
                        logs.add("Attempting Tier 1: Structured Output...")
                        try {
                            val prompt = JournalPrompts.getStructuredOutputPrompt(input)
                            val baseRequest =
                                GenerateContentRequest
                                    .Builder(TextPart(prompt))
                                    .apply {
                                        maxOutputTokens = 1024
                                        temperature = 0.0f
                                    }.build()
                            val typedRequest =
                                generateTypedContentRequest(
                                    generateContentRequest = baseRequest,
                                    outputClass = JournalIntent::class,
                                )
                            val response = generativeModel.generateContent(typedRequest)
                            val typed = response.candidates.firstOrNull()?.response
                            structuredResult = typed?.toString() ?: "No candidate response returned."
                            logs.add("Structured Output succeeded.")
                        } catch (e: Exception) {
                            structuredResult = "Failed: ${e.message}"
                            logs.add("Structured Output failed: ${e.message}")
                        }
                    } else {
                        structuredResult = "Feature.STRUCTURED_OUTPUT not supported on this model."
                    }

                    // Attempt Tier 2: Raw Text / JSON Prompt
                    logs.add("Attempting Tier 2: Raw JSON Prompt generation...")
                    try {
                        val jsonPrompt = JournalPrompts.getJsonPrompt(input)
                        val jsonRequest =
                            GenerateContentRequest
                                .Builder(TextPart(jsonPrompt))
                                .apply {
                                    maxOutputTokens = 1024
                                    temperature = 0.0f
                                }.build()
                        val textResponse = generativeModel.generateContent(jsonRequest)
                        rawText = textResponse.candidates.firstOrNull()?.text ?: "(Empty response from Nano)"
                        logs.add("Raw text generated (${rawText.length} chars).")
                    } catch (e: Exception) {
                        rawText = "Failed to generate text: ${e.message}"
                        logs.add("Raw text generation error: ${e.message}")
                    }
                } else {
                    logs.add("Model is NOT AVAILABLE. Skipping direct inference.")
                }

                // Step 2: Run through the full NanoJournalIntentExtractor pipeline
                logs.add("Running end-to-end NanoJournalIntentExtractor...")
                val extractionResult =
                    try {
                        nanoExtractor.extractIntent(input)
                    } catch (e: Exception) {
                        logs.add("NanoJournalIntentExtractor threw: ${e.message}")
                        null
                    }

                _uiState.update {
                    it.copy(
                        isTesting = false,
                        structuredOutputAttempt = structuredResult,
                        rawModelResponse = rawText,
                        parsedIntent = extractionResult?.intent,
                        engineUsed = extractionResult?.engine ?: "None (failed)",
                        executionLog = logs,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        generativeModel.close()
        nanoExtractor.release()
    }
}
