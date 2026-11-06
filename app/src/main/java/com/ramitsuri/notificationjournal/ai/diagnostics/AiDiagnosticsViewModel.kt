package com.ramitsuri.notificationjournal.ai.diagnostics

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Feature
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateTypedContentRequest
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.ramitsuri.notificationjournal.ai.extractor.NanoJournalIntentExtractor
import com.ramitsuri.notificationjournal.ai.model.JournalIntent
import com.ramitsuri.notificationjournal.ai.prompt.JournalPrompts
import com.ramitsuri.notificationjournal.ai.voice.SpeechEngine
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

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
    val isListeningToSpeech: Boolean = false,
    val autoRunExtractionAfterSpeech: Boolean = true,
    val structuredOutputAttempt: String? = null,
    val rawModelResponse: String? = null,
    val parsedIntent: JournalIntent? = null,
    val engineUsed: String? = null,
    val executionLog: List<String> = emptyList(),
    val isDownloading: Boolean = false,
    val downloadStatus: String? = null,
    val selectedSpeechEngine: SpeechEngine = SpeechEngine.PLATFORM,
    val mlKitSpeechStatus: Int = -1,
    val isDownloadingSpeech: Boolean = false,
    val speechDownloadStatus: String? = null,
    val customPromptRules: String = "",
    val customRulesSavedMessage: Boolean = false,
)

class AiDiagnosticsViewModel
    @Inject
    constructor(
        private val prefManager: PrefManager,
        private val generativeModel: GenerativeModel = Generation.getClient(),
        private val nanoExtractor: NanoJournalIntentExtractor = NanoJournalIntentExtractor(prefManager = prefManager),
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DiagnosticsUiState())
        val uiState = _uiState.asStateFlow()

        private var speechRecognizer: SpeechRecognizer? = null

        private fun getSpeechRecognizer(): SpeechRecognizer? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return null
            }
            speechRecognizer?.let { return it }
            return try {
                val options =
                    SpeechRecognizerOptions
                        .builder()
                        .apply {
                            preferredMode = SpeechRecognizerOptions.Mode.MODE_BASIC
                            locale = Locale.getDefault()
                        }.build()
                SpeechRecognition.getClient(options).also {
                    speechRecognizer = it
                }
            } catch (e: Exception) {
                android.util.Log.e("AiDiagnostics", "Failed to create SpeechRecognizer", e)
                null
            }
        }

        init {
            refreshModelInfo()
            viewModelScope.launch {
                prefManager.getCustomPromptRulesFlow().collect { rules ->
                    _uiState.update { it.copy(customPromptRules = rules) }
                }
            }
        }

        fun onTestInputChange(input: String) {
            _uiState.update { it.copy(testInput = input) }
        }

        fun onCustomPromptRulesChange(rules: String) {
            _uiState.update { it.copy(customPromptRules = rules, customRulesSavedMessage = false) }
        }

        fun saveCustomPromptRules() {
            val rules = _uiState.value.customPromptRules
            viewModelScope.launch {
                prefManager.setCustomPromptRules(rules)
                _uiState.update { it.copy(customRulesSavedMessage = true) }
            }
        }

        fun onSpeechEngineSelected(engine: SpeechEngine) {
            viewModelScope.launch {
                prefManager.setSpeechEngine(engine.id)
                _uiState.update { it.copy(selectedSpeechEngine = engine) }
            }
        }

        fun refreshModelInfo() {
            viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true) }
                val (info, speechStatus, currentEngine) =
                    withContext(Dispatchers.IO) {
                        val status =
                            try {
                                generativeModel.checkStatus()
                            } catch (_: Exception) {
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

                        val modelInfo =
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

                        val speechStat =
                            try {
                                val stat = getSpeechRecognizer()?.checkStatus() ?: -1
                                android.util.Log.d("AiDiagnostics", "SpeechRecognizer checkStatus returned: $stat")
                                stat
                            } catch (e: Exception) {
                                android.util.Log.e("AiDiagnostics", "SpeechRecognizer checkStatus threw exception", e)
                                -1
                            }

                        val savedEngineId = prefManager.getSpeechEngineFlow().first()
                        val activeEngine = SpeechEngine.fromId(savedEngineId)

                        Triple(modelInfo, speechStat, activeEngine)
                    }

                _uiState.update {
                    it.copy(
                        modelInfo = info,
                        mlKitSpeechStatus = speechStatus,
                        selectedSpeechEngine = currentEngine,
                        isRefreshing = false,
                    )
                }
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

        fun triggerSpeechModelDownload() {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isDownloadingSpeech = true,
                        speechDownloadStatus = "Initiating speech model download...",
                    )
                }
                try {
                    val recognizer = getSpeechRecognizer()
                    if (recognizer == null) {
                        _uiState.update {
                            it.copy(speechDownloadStatus = "ML Kit GenAI Speech is not available on this device")
                        }
                        return@launch
                    }
                    recognizer.download().collect { status ->
                        _uiState.update { it.copy(speechDownloadStatus = status.toString()) }
                    }
                    refreshModelInfo()
                } catch (e: Exception) {
                    _uiState.update { it.copy(speechDownloadStatus = "Download error: ${e.message}") }
                } finally {
                    _uiState.update { it.copy(isDownloadingSpeech = false) }
                }
            }
        }

        fun onSpeechListeningStarted() {
            _uiState.update {
                it.copy(
                    isListeningToSpeech = true,
                    testInput = "",
                    executionLog = listOf("Listening on mic (${it.selectedSpeechEngine.displayName})..."),
                )
            }
        }

        fun onSpeechListeningStopped() {
            _uiState.update { it.copy(isListeningToSpeech = false) }
        }

        fun onSpeechError(errorMessage: String) {
            _uiState.update {
                it.copy(
                    isListeningToSpeech = false,
                    executionLog = it.executionLog + "Speech recognition error: $errorMessage",
                )
            }
        }

        fun onSpeechFinalResult(
            text: String,
            engineName: String,
            durationMs: Long,
        ) {
            val cleanedText = text.trim().removeSuffix(".")
            val autoRun = _uiState.value.autoRunExtractionAfterSpeech
            _uiState.update {
                it.copy(
                    isListeningToSpeech = false,
                    testInput = cleanedText,
                    executionLog = it.executionLog + "Transcribed in ${durationMs}ms via $engineName: \"$cleanedText\"",
                )
            }
            if (autoRun && cleanedText.isNotBlank()) {
                runLiveTest(inputOverride = cleanedText)
            }
        }

        fun toggleAutoRunExtraction(enabled: Boolean) {
            _uiState.update { it.copy(autoRunExtractionAfterSpeech = enabled) }
        }

        fun runLiveTest(inputOverride: String? = null) {
            val input = inputOverride ?: _uiState.value.testInput
            if (input.isBlank()) return

            viewModelScope.launch {
                val existingLogs = if (inputOverride != null) _uiState.value.executionLog else emptyList()
                _uiState.update {
                    it.copy(
                        testInput = input,
                        isTesting = true,
                        structuredOutputAttempt = null,
                        rawModelResponse = null,
                        parsedIntent = null,
                        engineUsed = null,
                        executionLog = existingLogs,
                    )
                }

                val logs = existingLogs.toMutableList()
                var structuredResult: String? = null
                var rawText: String? = null

                withContext(Dispatchers.IO) {
                    val nluStartTime = System.currentTimeMillis()

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

                        val customRules = _uiState.value.customPromptRules

                        // Attempt Tier 1: Structured Output
                        if (hasStructured) {
                            logs.add("Attempting Tier 1: Structured Output...")
                            try {
                                val prompt = JournalPrompts.getStructuredOutputPrompt(input, customRules = customRules)
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
                            val jsonPrompt = JournalPrompts.getJsonPrompt(input, customRules = customRules)
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

                    val nluDuration = System.currentTimeMillis() - nluStartTime
                    logs.add("NLU intent extraction finished in ${nluDuration}ms.")

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
            try {
                speechRecognizer?.close()
            } catch (_: Exception) {
            }
        }
    }
