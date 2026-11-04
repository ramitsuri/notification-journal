package com.ramitsuri.notificationjournal.ai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.ai.model.JournalIntent
import com.ramitsuri.notificationjournal.ai.pipeline.VoiceAssistantPipeline
import com.ramitsuri.notificationjournal.ai.pipeline.VoiceAssistantState
import com.ramitsuri.notificationjournal.ai.voice.SpeechEngine
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

sealed interface AssistantUiState {
    data object Idle : AssistantUiState

    data class Listening(
        val partialText: String = "",
        val hasSpeechStarted: Boolean = false,
    ) : AssistantUiState

    data class Processing(
        val transcription: String,
    ) : AssistantUiState

    data class Success(
        val message: String,
        val matchedEntries: List<JournalEntry> = emptyList(),
        val engine: String = "",
        val speechEngine: String = "",
    ) : AssistantUiState

    data class Error(
        val error: String,
    ) : AssistantUiState
}

class JournalAssistantViewModel(
    private val repository: JournalRepository = ServiceLocator.repository,
    private val pipeline: VoiceAssistantPipeline = VoiceAssistantPipeline(),
    prefManager: PrefManager = ServiceLocator.prefManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AssistantUiState>(AssistantUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var voiceSessionJob: Job? = null

    val speechEngine: StateFlow<SpeechEngine> =
        prefManager
            .getSpeechEngineFlow()
            .map { SpeechEngine.fromId(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = SpeechEngine.PLATFORM,
            )

    fun startVoiceSession(context: Context) {
        stopVoiceSession()
        voiceSessionJob =
            viewModelScope.launch {
                val engine = speechEngine.value
                pipeline.startSession(context, engine).collect { state ->
                    when (state) {
                        VoiceAssistantState.Idle -> {
                            _uiState.value = AssistantUiState.Idle
                        }

                        is VoiceAssistantState.Listening -> {
                            _uiState.value =
                                AssistantUiState.Listening(
                                    partialText = state.partialText,
                                    hasSpeechStarted = state.hasSpeechStarted,
                                )
                        }

                        is VoiceAssistantState.Extracting -> {
                            _uiState.value =
                                AssistantUiState.Processing(
                                    transcription = state.transcription,
                                )
                        }

                        is VoiceAssistantState.Completed -> {
                            val extraction = state.extractionResult
                            if (extraction == null) {
                                _uiState.value = AssistantUiState.Error("Could not understand your request.")
                            } else {
                                handleIntent(
                                    intent = extraction.intent,
                                    engine = extraction.engine,
                                    speechEngine = state.speechEngine.displayName,
                                )
                            }
                        }

                        is VoiceAssistantState.Error -> {
                            _uiState.value = AssistantUiState.Error(state.message)
                        }
                    }
                }
            }
    }

    fun stopVoiceSession() {
        voiceSessionJob?.cancel()
        voiceSessionJob = null
        _uiState.value = AssistantUiState.Idle
    }

    fun processQuery(
        input: String,
        speechEngineName: String = "",
    ) {
        if (input.isBlank()) return
        viewModelScope.launch {
            _uiState.value = AssistantUiState.Processing(input)
            try {
                val result =
                    withContext(Dispatchers.Default) {
                        pipeline.processText(input)
                    }

                if (result == null) {
                    _uiState.value = AssistantUiState.Error("Could not understand your request.")
                    return@launch
                }

                handleIntent(result.intent, result.engine, speechEngineName)
            } catch (e: Exception) {
                _uiState.value = AssistantUiState.Error("Error processing request: ${e.message}")
            }
        }
    }

    private suspend fun handleIntent(
        intent: JournalIntent,
        engine: String,
        speechEngine: String,
    ) {
        withContext(Dispatchers.IO) {
            when (intent.action) {
                "ADD_ENTRY" -> {
                    val text = intent.entryText
                    if (text.isNullOrBlank()) {
                        _uiState.value = AssistantUiState.Error("No entry content was recognized.")
                        return@withContext
                    }
                    val targetTime =
                        intent.targetDate?.let { dateStr ->
                            try {
                                val parsedDate = LocalDate.parse(dateStr)
                                LocalDateTime(parsedDate, LocalTime(12, 0))
                            } catch (e: Exception) {
                                null
                            }
                        }
                    // Leave tag as null during testing so new entries remain untagged and appear at the top for review
                    val tagToUse: String? = null
                    if (targetTime != null) {
                        repository.insert(text = text, tag = tagToUse, time = targetTime)
                    } else {
                        repository.insert(text = text, tag = tagToUse)
                    }
                    val reply = intent.conversationalReply ?: "Saved to journal: \"$text\""
                    _uiState.value =
                        AssistantUiState.Success(
                            message = reply,
                            engine = engine,
                            speechEngine = speechEngine,
                        )
                }

                "QUERY_ENTRIES" -> {
                    val entries = queryDatabase(intent.targetDate, intent.searchKeyword)
                    if (entries.isEmpty()) {
                        _uiState.value =
                            AssistantUiState.Success(
                                message = "No matching entries found.",
                                engine = engine,
                                speechEngine = speechEngine,
                            )
                    } else {
                        val reply = intent.conversationalReply ?: "Found ${entries.size} matching entry/entries."
                        _uiState.value =
                            AssistantUiState.Success(
                                message = reply,
                                matchedEntries = entries,
                                engine = engine,
                                speechEngine = speechEngine,
                            )
                    }
                }

                else -> {
                    _uiState.value =
                        AssistantUiState.Success(
                            message = intent.conversationalReply ?: "I didn't detect an entry to add or query.",
                            engine = engine,
                            speechEngine = speechEngine,
                        )
                }
            }
        }
    }

    private suspend fun queryDatabase(
        dateStr: String?,
        keyword: String?,
    ): List<JournalEntry> {
        val dateEntries =
            if (!dateStr.isNullOrBlank()) {
                try {
                    val parsed = LocalDate.parse(dateStr)
                    repository.getForDateFlow(parsed).first()
                } catch (e: Exception) {
                    repository.getAll()
                }
            } else {
                repository.getAll()
            }

        return if (!keyword.isNullOrBlank()) {
            dateEntries.filter { it.text.contains(keyword, ignoreCase = true) }
        } else {
            dateEntries
        }
    }

    override fun onCleared() {
        super.onCleared()
        pipeline.release()
    }

    companion object {
        fun factory(
            repository: JournalRepository = ServiceLocator.repository,
            pipeline: VoiceAssistantPipeline = VoiceAssistantPipeline(),
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    JournalAssistantViewModel(repository, pipeline) as T
            }
    }
}
