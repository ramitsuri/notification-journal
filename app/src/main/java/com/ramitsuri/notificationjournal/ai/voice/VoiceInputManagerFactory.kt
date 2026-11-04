package com.ramitsuri.notificationjournal.ai.voice

import android.content.Context
import android.os.Build

object VoiceInputManagerFactory {
    fun create(
        context: Context,
        engine: SpeechEngine,
    ): VoiceInputManager =
        when (engine) {
            SpeechEngine.MLKIT_GENAI -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MlKitGenAiVoiceInputManager()
                } else {
                    PlatformVoiceInputManager(context = context)
                }
            }

            SpeechEngine.PLATFORM -> {
                PlatformVoiceInputManager(context = context)
            }
        }
}
