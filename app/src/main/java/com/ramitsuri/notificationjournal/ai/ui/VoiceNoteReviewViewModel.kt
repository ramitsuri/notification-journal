package com.ramitsuri.notificationjournal.ai.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.ai.voice.PcmAudioPlayer
import com.ramitsuri.notificationjournal.core.data.VoiceNoteDao
import com.ramitsuri.notificationjournal.core.model.voice.VoiceNote
import com.ramitsuri.notificationjournal.core.model.voice.VoiceNoteStatus
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.work.VoiceNoteProcessWorker
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class VoiceNoteReviewViewModel
    @Inject
    constructor(
        private val repository: JournalRepository,
        private val voiceNoteDao: VoiceNoteDao,
        private val player: PcmAudioPlayer = PcmAudioPlayer(),
    ) : ViewModel() {
        val isPlayingFlow: StateFlow<Boolean> = player.isPlayingFlow

        fun playAudio(file: File) {
            viewModelScope.launch {
                player.playPcm(file)
            }
        }

        fun stopAudio() {
            player.stop()
        }

        fun save(
            voiceNote: VoiceNote,
            text: String,
            onComplete: () -> Unit,
        ) {
            viewModelScope.launch {
                player.stop()
                repository.insert(
                    text = text.trim(),
                    tag = null,
                    time = voiceNote.recordedAt,
                )
                voiceNoteDao.delete(voiceNote.id)
                File(voiceNote.filePath).delete()
                onComplete()
            }
        }

        fun delete(
            voiceNote: VoiceNote,
            onComplete: () -> Unit,
        ) {
            viewModelScope.launch {
                player.stop()
                voiceNoteDao.delete(voiceNote.id)
                File(voiceNote.filePath).delete()
                onComplete()
            }
        }

        fun retry(
            context: Context,
            voiceNote: VoiceNote,
            onComplete: () -> Unit,
        ) {
            viewModelScope.launch {
                player.stop()
                voiceNoteDao.updateStatus(
                    voiceNote.id,
                    VoiceNoteStatus.PROCESSING,
                    null,
                )
                VoiceNoteProcessWorker.enqueue(context)
                onComplete()
            }
        }

        override fun onCleared() {
            player.stop()
        }
    }
