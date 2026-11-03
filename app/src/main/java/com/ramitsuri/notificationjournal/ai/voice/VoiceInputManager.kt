package com.ramitsuri.notificationjournal.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Manages 100% on-device, private voice transcription.
 * Uses Android's on-device SpeechRecognizer exclusively so no audio data ever leaves the device.
 */
class VoiceInputManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun isOnDeviceAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    fun startListening() {
        mainHandler.post {
            cleanUp()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !isOnDeviceAvailable()) {
                onError("On-device speech recognition requires Android 12+ and is not available on this device.")
                return@post
            }

            // Exclusively use on-device recognition to ensure zero data leaves the device
            val speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)

            speechRecognizer.setRecognitionListener(
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}

                    override fun onBeginningOfSpeech() {}

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
                        onError(message)
                    }

                    override fun onResults(results: Bundle?) {
                        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!text.isNullOrBlank()) {
                            onResult(text)
                        } else {
                            onError("No transcription available.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}

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

            recognizer = speechRecognizer
            speechRecognizer.startListening(intent)
        }
    }

    fun stopListening() {
        mainHandler.post {
            cleanUp()
        }
    }

    private fun cleanUp() {
        recognizer?.let {
            try {
                it.stopListening()
                it.cancel()
                it.destroy()
            } catch (_: Exception) {
                // Ignore cleanup errors
            }
        }
        recognizer = null
    }
}
