package com.ramitsuri.notificationjournal.ai.pipeline

import android.content.Context
import com.ramitsuri.notificationjournal.ai.extractor.IntentExtractorFactory
import com.ramitsuri.notificationjournal.ai.extractor.JournalIntentExtractor
import com.ramitsuri.notificationjournal.ai.model.ExtractionResult
import com.ramitsuri.notificationjournal.ai.voice.SpeechEngine
import com.ramitsuri.notificationjournal.ai.voice.VoiceInputEvent
import com.ramitsuri.notificationjournal.ai.voice.VoiceInputManagerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.withContext

sealed interface VoiceAssistantState {
    data object Idle : VoiceAssistantState

    data class Listening(
        val partialText: String = "",
        val hasSpeechStarted: Boolean = false,
        val speechEngine: SpeechEngine,
    ) : VoiceAssistantState

    data class Extracting(
        val transcription: String,
        val speechEngine: SpeechEngine,
        val speechDurationMs: Long,
    ) : VoiceAssistantState

    data class Completed(
        val transcription: String,
        val extractionResult: ExtractionResult?,
        val speechEngine: SpeechEngine,
        val speechDurationMs: Long,
        val nluDurationMs: Long,
    ) : VoiceAssistantState

    data class Error(
        val message: String,
        val stage: Stage,
    ) : VoiceAssistantState {
        enum class Stage {
            SPEECH,
            EXTRACTION,
        }
    }
}

/**
 * End-to-end voice assistant coordinator that manages mic audio listening,
 * speech-to-text recognition, and NLU intent extraction in a single pipeline.
 */
class VoiceAssistantPipeline(
    private val voiceInputManagerFactory: VoiceInputManagerFactory = VoiceInputManagerFactory,
    private val extractor: JournalIntentExtractor = IntentExtractorFactory.create(),
) {
    fun startSession(
        context: Context,
        speechEngine: SpeechEngine,
    ): Flow<VoiceAssistantState> =
        flow {
            emit(VoiceAssistantState.Listening(speechEngine = speechEngine))

            val voiceManager = voiceInputManagerFactory.create(context, speechEngine)
            val speechStartTime = System.currentTimeMillis()
            var finalTranscription: String? = null
            var speechDuration = 0L

            voiceManager
                .startListening()
                .transformWhile { event ->
                    when (event) {
                        VoiceInputEvent.SpeechStarted -> {
                            emit(
                                VoiceAssistantState.Listening(
                                    hasSpeechStarted = true,
                                    speechEngine = speechEngine,
                                ),
                            )
                            true
                        }

                        is VoiceInputEvent.PartialResult -> {
                            emit(
                                VoiceAssistantState.Listening(
                                    partialText = event.text,
                                    hasSpeechStarted = true,
                                    speechEngine = speechEngine,
                                ),
                            )
                            true
                        }

                        is VoiceInputEvent.FinalResult -> {
                            speechDuration = System.currentTimeMillis() - speechStartTime
                            finalTranscription = event.text
                            false
                        }

                        is VoiceInputEvent.Error -> {
                            emit(
                                VoiceAssistantState.Error(
                                    message = event.message,
                                    stage = VoiceAssistantState.Error.Stage.SPEECH,
                                ),
                            )
                            false
                        }
                    }
                }.collect()

            val text = finalTranscription
            if (text.isNullOrBlank()) {
                return@flow
            }

            emit(
                VoiceAssistantState.Extracting(
                    transcription = text,
                    speechEngine = speechEngine,
                    speechDurationMs = speechDuration,
                ),
            )

            val nluStartTime = System.currentTimeMillis()
            val intentResult =
                try {
                    withContext(Dispatchers.Default) {
                        extractor.extractIntent(text)
                    }
                } catch (e: Exception) {
                    emit(
                        VoiceAssistantState.Error(
                            message = "Extraction failed: ${e.message}",
                            stage = VoiceAssistantState.Error.Stage.EXTRACTION,
                        ),
                    )
                    return@flow
                }
            val nluDuration = System.currentTimeMillis() - nluStartTime

            emit(
                VoiceAssistantState.Completed(
                    transcription = text,
                    extractionResult = intentResult,
                    speechEngine = speechEngine,
                    speechDurationMs = speechDuration,
                    nluDurationMs = nluDuration,
                ),
            )
        }

    suspend fun processText(input: String): ExtractionResult? = extractor.extractIntent(input)

    fun release() {
        extractor.release()
    }
}
