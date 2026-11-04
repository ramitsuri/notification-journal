package com.ramitsuri.notificationjournal.ai.voice

sealed interface VoiceInputEvent {
    data object SpeechStarted : VoiceInputEvent

    data class PartialResult(
        val text: String,
    ) : VoiceInputEvent

    data class FinalResult(
        val text: String,
    ) : VoiceInputEvent

    data class Error(
        val message: String,
    ) : VoiceInputEvent
}
