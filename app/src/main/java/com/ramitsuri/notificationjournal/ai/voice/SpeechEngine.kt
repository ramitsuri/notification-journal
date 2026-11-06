package com.ramitsuri.notificationjournal.ai.voice

enum class SpeechEngine(
    val id: String,
    val displayName: String,
    val description: String,
) {
    PLATFORM(
        id = "platform",
        displayName = "Platform SpeechRecognizer",
        description = "Uses Android's built-in on-device speech service. Fast and works on any Android 12+ device.",
    ),
    MLKIT_GENAI(
        id = "mlkit_genai",
        displayName = "ML Kit GenAI Speech (Basic)",
        description =
            "Uses ML Kit Speech Recognition in Basic mode via Google Speech Services " +
                "with reactive streaming and word timing.",
    ),
    ;

    companion object {
        fun fromId(id: String): SpeechEngine = entries.firstOrNull { it.id == id } ?: PLATFORM
    }
}
