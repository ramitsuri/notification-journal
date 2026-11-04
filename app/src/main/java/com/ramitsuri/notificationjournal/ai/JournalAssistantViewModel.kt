package com.ramitsuri.notificationjournal.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.ai.extractor.IntentExtractorFactory
import com.ramitsuri.notificationjournal.ai.extractor.JournalIntentExtractor
import com.ramitsuri.notificationjournal.ai.model.JournalIntent
import com.ramitsuri.notificationjournal.ai.voice.SpeechEngine
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import kotlinx.coroutines.Dispatchers
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
    private val extractor: JournalIntentExtractor = IntentExtractorFactory.create(),
    prefManager: PrefManager = ServiceLocator.prefManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AssistantUiState>(AssistantUiState.Idle)
    val uiState = _uiState.asStateFlow()

    val speechEngine: StateFlow<SpeechEngine> =
        prefManager
            .getSpeechEngineFlow()
            .map { SpeechEngine.fromId(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = SpeechEngine.PLATFORM,
            )

    fun onListeningStarted() {
        _uiState.value = AssistantUiState.Listening()
    }

    fun onSpeechStarted() {
        val current = _uiState.value
        if (current is AssistantUiState.Listening) {
            _uiState.value = current.copy(hasSpeechStarted = true)
        } else {
            _uiState.value = AssistantUiState.Listening(hasSpeechStarted = true)
        }
    }

    fun onPartialTranscription(text: String) {
        _uiState.value = AssistantUiState.Listening(partialText = text, hasSpeechStarted = true)
    }

    fun onError(message: String) {
        _uiState.value = AssistantUiState.Error(message)
    }

    fun resetState() {
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
                        extractor.extractIntent(input)
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
        extractor.release()
    }

    companion object {
        fun factory(
            repository: JournalRepository = ServiceLocator.repository,
            extractor: JournalIntentExtractor = IntentExtractorFactory.create(),
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    JournalAssistantViewModel(repository, extractor) as T
            }
    }
}
