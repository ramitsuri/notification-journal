package com.ramitsuri.notificationjournal.work

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerRequest
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.ai.extractor.IntentExtractorFactory
import com.ramitsuri.notificationjournal.core.data.VoiceNoteDao
import com.ramitsuri.notificationjournal.core.deeplink.homeDeepLink
import com.ramitsuri.notificationjournal.core.model.voice.VoiceNote
import com.ramitsuri.notificationjournal.core.model.voice.VoiceNoteStatus
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelType
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.ramitsuri.notificationjournal.core.utils.NotificationInfo
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.collect
import java.io.File
import java.util.Locale

private const val TAG = "VoiceNoteWorker"
private const val NOTIFICATION_ID = 401
private const val MIN_AUDIO_BYTES = 10_000L

class VoiceNoteProcessWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val voiceNoteDao: VoiceNoteDao,
        private val repository: JournalRepository,
        private val notificationHandler: NotificationHandler,
        private val prefManager: PrefManager,
    ) : CoroutineWorker(context, params) {
        @AssistedFactory
        interface Factory {
            fun create(
                context: Context,
                params: WorkerParameters,
            ): VoiceNoteProcessWorker
        }

        override suspend fun doWork(): Result {
            val pendingNotes = voiceNoteDao.getPendingNotes()
            if (pendingNotes.isEmpty()) {
                return Result.success()
            }

            for (note in pendingNotes) {
                processNote(note)
            }

            return Result.success()
        }

        private suspend fun processNote(note: VoiceNote) {
            val file = File(note.filePath)

            if (!file.exists() || file.length() < MIN_AUDIO_BYTES) {
                Log.e(TAG, "Audio file missing or too small for note ${note.id}: length=${file.length()}")
                voiceNoteDao.updateStatus(note.id, VoiceNoteStatus.FAILED, "Audio file is empty or missing")
                postFailureNotification()
                return
            }

            val rawTranscribedText =
                try {
                    transcribeFile(file)
                } catch (e: Exception) {
                    Log.e(TAG, "Transcription failed for note ${note.id}", e)
                    null
                }

            if (rawTranscribedText.isNullOrBlank()) {
                Log.e(TAG, "Transcription returned empty text for note ${note.id}")
                voiceNoteDao.updateStatus(note.id, VoiceNoteStatus.FAILED, "No speech recognized in audio")
                postFailureNotification()
                return
            }

            val transcribedText = rawTranscribedText.trim().removeSuffix(".")

            val extractor = IntentExtractorFactory.create(prefManager)
            val intent =
                try {
                    val result = extractor.extractIntent(transcribedText)
                    result?.intent
                } catch (e: Exception) {
                    Log.w(TAG, "Intent extraction failed; using raw transcription", e)
                    null
                }

            val entryText = intent?.entryText?.ifBlank { transcribedText } ?: transcribedText

            val entryTime =
                com.ramitsuri.notificationjournal.core.utils.resolveEntryTime(
                    targetDateStr = intent?.targetDate,
                    targetTimeStr = intent?.targetTime,
                    fallback = note.recordedAt,
                )

            try {
                repository.insert(
                    text = entryText,
                    tag = null,
                    time = entryTime,
                )
                file.delete()
                voiceNoteDao.delete(note.id)
                Log.d(TAG, "Successfully processed and saved voice note ${note.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed inserting journal entry for note ${note.id}", e)
                voiceNoteDao.updateStatus(note.id, VoiceNoteStatus.FAILED, "Failed to save journal entry: ${e.message}")
                postFailureNotification()
            }
        }

        private suspend fun transcribeFile(file: File): String? {
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
                    Log.e(TAG, "checkStatus failed in worker", e)
                    FeatureStatus.UNAVAILABLE
                }

            if (status == FeatureStatus.DOWNLOADABLE) {
                try {
                    recognizer.download().collect()
                } catch (e: Exception) {
                    Log.e(TAG, "Model download failed in worker", e)
                    recognizer.close()
                    return null
                }
            } else if (status != FeatureStatus.AVAILABLE) {
                Log.e(TAG, "ML Kit Speech not available: status=$status")
                recognizer.close()
                return null
            }

            val audioSource =
                try {
                    AudioSource.fromFile(file)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create AudioSource from file", e)
                    recognizer.close()
                    return null
                }

            val request =
                SpeechRecognizerRequest
                    .builder()
                    .apply {
                        this.audioSource = audioSource
                    }.build()

            var recognizedText = ""
            var errorEncountered: String? = null

            try {
                recognizer.startRecognition(request).collect { response ->
                    when (response) {
                        is SpeechRecognizerResponse.PartialTextResponse -> {
                            if (response.text.isNotBlank()) {
                                recognizedText = response.text
                            }
                        }

                        is SpeechRecognizerResponse.FinalTextResponse -> {
                            if (response.text.isNotBlank()) {
                                recognizedText = response.text
                            }
                        }

                        is SpeechRecognizerResponse.ErrorResponse -> {
                            errorEncountered = response.e.message ?: "Recognition error"
                            Log.e(TAG, "Recognition error response: $errorEncountered")
                        }

                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "startRecognition failed in worker", e)
            } finally {
                try {
                    audioSource.close()
                } catch (_: Exception) {
                }
                try {
                    recognizer.close()
                } catch (_: Exception) {
                }
            }

            return recognizedText.ifBlank { null }
        }

        private fun postFailureNotification() {
            val notificationInfo =
                NotificationInfo(
                    channel = NotificationChannelType.MAIN,
                    title = applicationContext.getString(R.string.voice_note_failed_notification_title),
                    body = applicationContext.getString(R.string.voice_note_failed_notification_text),
                    iconResId = R.drawable.ic_notification,
                    isVisibilityPublic = true,
                    cancelOnTouch = true,
                    isForegroundServiceImmediate = false,
                    isOngoing = false,
                    clickDeepLinkUri = homeDeepLink,
                    actions = emptyList(),
                    actionExtras = emptyMap(),
                )
            notificationHandler.showNotification(notificationInfo)
        }

        override suspend fun getForegroundInfo(): ForegroundInfo {
            val notification =
                NotificationCompat
                    .Builder(
                        applicationContext,
                        NotificationChannelType.MAIN.id,
                    ).apply {
                        setSmallIcon(R.drawable.ic_notification)
                        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        setContentTitle(
                            applicationContext.getString(R.string.voice_note_worker_notification_title),
                        )
                    }.build()
            return ForegroundInfo(NOTIFICATION_ID, notification)
        }

        companion object {
            const val WORK_TAG = "TAG-VoiceNoteProcessWorker"

            fun enqueue(context: Context) {
                val request: OneTimeWorkRequest =
                    OneTimeWorkRequestBuilder<VoiceNoteProcessWorker>()
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .addTag(WORK_TAG)
                        .build()
                WorkManager.getInstance(context).enqueue(request)
            }
        }
    }
