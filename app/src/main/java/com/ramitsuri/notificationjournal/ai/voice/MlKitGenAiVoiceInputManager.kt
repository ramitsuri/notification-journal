package com.ramitsuri.notificationjournal.ai.voice

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerRequest
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "MlKitGenAiSpeech"
private val SILENCE_TIMEOUT = 1500.milliseconds
private val INITIAL_SPEECH_TIMEOUT = 6000.milliseconds

/**
 * Manages on-device voice transcription using ML Kit's GenAI Speech Recognition API.
 * Uses Google Speech Services via MODE_BASIC with automatic silence endpointing.
 */
@RequiresApi(Build.VERSION_CODES.S)
class MlKitGenAiVoiceInputManager : VoiceInputManager {
    override fun startListening(): Flow<VoiceInputEvent> =
        channelFlow {
            val options =
                SpeechRecognizerOptions
                    .builder()
                    .apply {
                        preferredMode = SpeechRecognizerOptions.Mode.MODE_BASIC
                        locale = Locale.getDefault()
                    }.build()

            val recognizer = SpeechRecognition.getClient(options)

            val status =
                try {
                    recognizer.checkStatus()
                } catch (e: Exception) {
                    Log.e(TAG, "checkStatus failed", e)
                    FeatureStatus.UNAVAILABLE
                }

            Log.d(TAG, "ML Kit GenAI Speech status: $status")

            if (status == FeatureStatus.DOWNLOADABLE) {
                try {
                    recognizer.download().collect { downloadStatus ->
                        Log.d(TAG, "Download progress: $downloadStatus")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Speech model download failed", e)
                    trySend(VoiceInputEvent.Error("ML Kit speech model needs download: ${e.message}"))
                    recognizer.close()
                    close()
                    return@channelFlow
                }
            } else if (status != FeatureStatus.AVAILABLE) {
                trySend(
                    VoiceInputEvent.Error(
                        "ML Kit GenAI Speech is not available on this device (status: $status).",
                    ),
                )
                recognizer.close()
                close()
                return@channelFlow
            }

            val audioSource = AudioSource.fromMic()
            val request =
                SpeechRecognizerRequest
                    .builder()
                    .apply {
                        this.audioSource = audioSource
                    }.build()

            var speechStarted = false
            var lastRecognizedText = ""
            var silenceJob: Job? = null

            // If user hasn't started speaking within timeout, finish with error
            val initialTimeoutJob =
                launch {
                    delay(INITIAL_SPEECH_TIMEOUT)
                    if (!speechStarted) {
                        trySend(VoiceInputEvent.Error("No speech detected. Try speaking again."))
                        close()
                    }
                }

            val recognitionJob =
                launch {
                    try {
                        recognizer.startRecognition(request).collect { response ->
                            when (response) {
                                is SpeechRecognizerResponse.PartialTextResponse -> {
                                    if (!speechStarted) {
                                        speechStarted = true
                                        initialTimeoutJob.cancel()
                                        trySend(VoiceInputEvent.SpeechStarted)
                                    }
                                    if (response.text.isNotBlank()) {
                                        lastRecognizedText = response.text
                                        trySend(VoiceInputEvent.PartialResult(response.text))

                                        // Reset silence detector: when user pauses for 1.5s, finalize
                                        silenceJob?.cancel()
                                        silenceJob =
                                            this@channelFlow.launch {
                                                delay(SILENCE_TIMEOUT)
                                                Log.d(TAG, "Silence detected, finalizing: $lastRecognizedText")
                                                trySend(VoiceInputEvent.FinalResult(lastRecognizedText))
                                                close()
                                            }
                                    }
                                }

                                is SpeechRecognizerResponse.FinalTextResponse -> {
                                    initialTimeoutJob.cancel()
                                    silenceJob?.cancel()
                                    val finalText = response.text.ifBlank { lastRecognizedText }
                                    if (finalText.isNotBlank()) {
                                        trySend(VoiceInputEvent.FinalResult(finalText))
                                    } else {
                                        trySend(VoiceInputEvent.Error("No speech recognized."))
                                    }
                                    close()
                                }

                                is SpeechRecognizerResponse.ErrorResponse -> {
                                    initialTimeoutJob.cancel()
                                    silenceJob?.cancel()
                                    Log.e(TAG, "ErrorResponse: ${response.e.message}")
                                    trySend(VoiceInputEvent.Error(response.e.message ?: "GenAI Speech error"))
                                    close()
                                }

                                else -> {}
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "startRecognition failed", e)
                        trySend(VoiceInputEvent.Error("Speech recognition error: ${e.message}"))
                        close()
                    }
                }

            awaitClose {
                initialTimeoutJob.cancel()
                silenceJob?.cancel()
                recognitionJob.cancel()
                try {
                    audioSource.close()
                } catch (_: Exception) {
                }
                try {
                    recognizer.close()
                } catch (_: Exception) {
                }
            }
        }
}
