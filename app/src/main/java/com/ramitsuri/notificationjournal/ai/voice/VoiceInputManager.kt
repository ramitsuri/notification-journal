package com.ramitsuri.notificationjournal.ai.voice

import kotlinx.coroutines.flow.Flow

/**
 * Common interface for on-device voice transcribers returning a reactive Flow of voice events.
 */
interface VoiceInputManager {
    fun startListening(): Flow<VoiceInputEvent>
}
