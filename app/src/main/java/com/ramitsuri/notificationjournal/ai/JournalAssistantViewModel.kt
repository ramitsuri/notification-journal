package com.ramitsuri.notificationjournal.ai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.ai.voice.PcmAudioRecorder
import com.ramitsuri.notificationjournal.core.data.VoiceNoteDao
import com.ramitsuri.notificationjournal.core.model.voice.VoiceNote
import com.ramitsuri.notificationjournal.core.model.voice.VoiceNoteStatus
import com.ramitsuri.notificationjournal.core.utils.nowLocal
import com.ramitsuri.notificationjournal.work.VoiceNoteProcessWorker
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock

sealed interface AssistantUiState {
    data object Idle : AssistantUiState

    data class Recording(
        val segmentIndex: Int = 1,
        val durationSeconds: Int = 0,
        val silentMicWarning: Boolean = false,
    ) : AssistantUiState

    data class Error(
        val error: String,
    ) : AssistantUiState
}

class JournalAssistantViewModel
    @Inject
    constructor(
        private val voiceNoteDao: VoiceNoteDao,
        private val recorder: PcmAudioRecorder = PcmAudioRecorder(),
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<AssistantUiState>(AssistantUiState.Idle)
        val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

        val amplitudeFlow: StateFlow<Float> = recorder.amplitudeFlow

        private var recordingJob: Job? = null
        private var timerJob: Job? = null
        private var durationSeconds = 0
        private var segmentIndex = 1

        init {
            viewModelScope.launch {
                recorder.silentMicWarningFlow.collect { silent ->
                    val current = _uiState.value
                    if (current is AssistantUiState.Recording) {
                        _uiState.value = current.copy(silentMicWarning = silent)
                    }
                }
            }
        }

        fun startRecording(context: Context) {
            segmentIndex = 1
            durationSeconds = 0
            _uiState.value =
                AssistantUiState.Recording(
                    segmentIndex = segmentIndex,
                    durationSeconds = durationSeconds,
                    silentMicWarning = false,
                )

            startTimer()

            recordingJob =
                viewModelScope.launch {
                    val success = recorder.record(context)
                    if (!success) {
                        stopTimer()
                        _uiState.value = AssistantUiState.Error("Could not access microphone.")
                    }
                }
        }

        fun nextEntry(context: Context) {
            viewModelScope.launch {
                val rotated = recorder.rotateSegment(context)
                if (rotated != null) {
                    segmentIndex++
                    durationSeconds = 0
                    _uiState.value =
                        AssistantUiState.Recording(
                            segmentIndex = segmentIndex,
                            durationSeconds = durationSeconds,
                            silentMicWarning = recorder.silentMicWarningFlow.value,
                        )
                }
            }
        }

        fun commitRecording(
            context: Context,
            onDismiss: () -> Unit,
        ) {
            viewModelScope.launch {
                stopTimer()
                val files = recorder.stop()
                recordingJob?.join()
                recordingJob = null

                val validFiles = files.filter { it.exists() && it.length() >= 10_000L }

                if (validFiles.isEmpty()) {
                    files.forEach { it.delete() }
                    _uiState.value = AssistantUiState.Error("Recording was too short or empty. Please try again.")
                    return@launch
                }

                val now = Clock.System.nowLocal()
                val notes =
                    validFiles.mapIndexed { index, file ->
                        VoiceNote(
                            filePath = file.absolutePath,
                            recordedAt = now,
                            segmentIndex = index,
                            status = VoiceNoteStatus.PROCESSING,
                        )
                    }

                voiceNoteDao.insertAll(notes)

                VoiceNoteProcessWorker.enqueue(context)
                onDismiss()
            }
        }

        fun discardRecording() {
            viewModelScope.launch {
                stopTimer()
                recorder.cancel()
                recordingJob?.join()
                recordingJob = null
                _uiState.value = AssistantUiState.Idle
            }
        }

        private fun startTimer() {
            stopTimer()
            timerJob =
                viewModelScope.launch {
                    while (isActive) {
                        delay(1000L)
                        durationSeconds++
                        val current = _uiState.value
                        if (current is AssistantUiState.Recording) {
                            _uiState.value = current.copy(durationSeconds = durationSeconds)
                        }
                    }
                }
        }

        private fun stopTimer() {
            timerJob?.cancel()
            timerJob = null
        }

        override fun onCleared() {
            super.onCleared()
            stopTimer()
            recordingJob?.cancel()
            recordingJob = null
        }
    }
