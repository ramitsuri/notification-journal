package com.ramitsuri.notificationjournal.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.util.Locale

/**
 * Manages 100% on-device, private voice transcription using Android's platform SpeechRecognizer.
 * Available on Android 12+ devices with Speech Services installed.
 */
class PlatformVoiceInputManager(
    private val context: Context,
) : VoiceInputManager {
    fun isOnDeviceAvailable(): Boolean =
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    override fun startListening(): Flow<VoiceInputEvent> =
        callbackFlow {
            if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.S) || !isOnDeviceAvailable()) {
                trySend(
                    VoiceInputEvent.Error(
                        "On-device speech recognition requires Android 12+ and is not available on this device.",
                    ),
                )
                close()
                return@callbackFlow
            }

            val speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)

            speechRecognizer.setRecognitionListener(
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}

                    override fun onBeginningOfSpeech() {
                        trySend(VoiceInputEvent.SpeechStarted)
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        val message =
                            when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking again."
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected (timed out)."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Please try again."
                                SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
                                    "On-device speech service disconnected (Error 11). " +
                                        "Please verify offline speech files are downloaded in Settings."
                                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
                                ->
                                    "Offline speech language pack not available for ${Locale.getDefault().displayLanguage}."
                                else -> "Speech recognition error ($error)"
                            }
                        trySend(VoiceInputEvent.Error(message))
                        close()
                    }

                    override fun onResults(results: Bundle?) {
                        val text =
                            results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                        if (!text.isNullOrBlank()) {
                            trySend(VoiceInputEvent.FinalResult(text))
                        } else {
                            trySend(VoiceInputEvent.Error("No transcription available."))
                        }
                        close()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val text =
                            partialResults
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                        if (!text.isNullOrBlank()) {
                            trySend(VoiceInputEvent.PartialResult(text))
                        }
                    }

                    override fun onEvent(
                        eventType: Int,
                        params: Bundle?,
                    ) {}
                },
            )

            val intent =
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }

            try {
                speechRecognizer.startListening(intent)
            } catch (e: Exception) {
                trySend(VoiceInputEvent.Error("Failed to start speech recognition: ${e.message}"))
                close()
                return@callbackFlow
            }

            awaitClose {
                try {
                    speechRecognizer.stopListening()
                    speechRecognizer.cancel()
                    speechRecognizer.destroy()
                } catch (_: Exception) {
                    // Ignore cleanup errors
                }
            }
        }.flowOn(Dispatchers.Main)
}
