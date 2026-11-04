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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.withContext
import java.util.Locale

private const val TAG = "MlKitGenAiSpeech"

/**
 * Manages on-device voice transcription using ML Kit's GenAI Speech Recognition API.
 * Uses AICore on-device Gemini speech models.
 */
@RequiresApi(Build.VERSION_CODES.S)
class MlKitGenAiVoiceInputManager : VoiceInputManager {
    override fun startListening(): Flow<VoiceInputEvent> =
        flow {
            val options =
                SpeechRecognizerOptions
                    .builder()
                    .apply {
                        preferredMode = SpeechRecognizerOptions.Mode.MODE_ADVANCED
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
                    emit(VoiceInputEvent.Error("ML Kit speech model needs download: ${e.message}"))
                    recognizer.close()
                    return@flow
                }
            } else if (status != FeatureStatus.AVAILABLE) {
                emit(
                    VoiceInputEvent.Error(
                        "ML Kit GenAI Speech is not available on this device (status: $status). Try switching to Platform SpeechRecognizer.",
                    ),
                )
                recognizer.close()
                return@flow
            }

            var speechStarted = false
            val audioSource = AudioSource.fromMic()
            val request =
                SpeechRecognizerRequest
                    .builder()
                    .apply {
                        this.audioSource = audioSource
                    }.build()

            try {
                recognizer
                    .startRecognition(request)
                    .catch { e ->
                        Log.e(TAG, "startRecognition stream error", e)
                        emit(VoiceInputEvent.Error("Speech recognition error: ${e.message}"))
                    }.transformWhile { response ->
                        when (response) {
                            is SpeechRecognizerResponse.PartialTextResponse -> {
                                if (!speechStarted) {
                                    speechStarted = true
                                    emit(VoiceInputEvent.SpeechStarted)
                                }
                                if (response.text.isNotBlank()) {
                                    emit(VoiceInputEvent.PartialResult(response.text))
                                }
                                true
                            }

                            is SpeechRecognizerResponse.FinalTextResponse -> {
                                if (response.text.isNotBlank()) {
                                    emit(VoiceInputEvent.FinalResult(response.text))
                                } else {
                                    emit(VoiceInputEvent.Error("No speech recognized."))
                                }
                                false
                            }

                            is SpeechRecognizerResponse.ErrorResponse -> {
                                Log.e(TAG, "ErrorResponse: ${response.e.message}")
                                emit(VoiceInputEvent.Error(response.e.message ?: "GenAI Speech error"))
                                false
                            }

                            else -> true
                        }
                    }.collect {}
            } catch (e: Exception) {
                Log.e(TAG, "startRecognition failed", e)
                emit(VoiceInputEvent.Error("Failed to start speech recognition: ${e.message}"))
            } finally {
                withContext(NonCancellable) {
                    try {
                        audioSource.close()
                    } catch (_: Exception) {
                    }
                    try {
                        recognizer.stopRecognition()
                    } catch (_: Exception) {
                    }
                    try {
                        recognizer.close()
                    } catch (_: Exception) {
                    }
                }
            }
        }
}
