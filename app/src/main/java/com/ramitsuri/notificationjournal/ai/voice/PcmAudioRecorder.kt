package com.ramitsuri.notificationjournal.ai.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.sqrt

private const val TAG = "PcmAudioRecorder"
const val SAMPLE_RATE = 16000
const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
private const val SILENCE_WARNING_THRESHOLD_MS = 3000L

class PcmAudioRecorder(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private sealed interface Command {
        data class WriteData(
            val bytes: ByteArray,
        ) : Command

        data class Rotate(
            val newFile: File,
            val deferred: CompletableDeferred<File>,
        ) : Command

        data class Stop(
            val deferred: CompletableDeferred<List<File>>,
        ) : Command

        data class Cancel(
            val deferred: CompletableDeferred<Unit>,
        ) : Command
    }

    private var audioRecord: AudioRecord? = null
    private var commandChannel: Channel<Command>? = null

    private val completedFiles = mutableListOf<File>()
    private var currentActiveFile: File? = null

    private val _amplitudeFlow = MutableStateFlow(0f)
    val amplitudeFlow: StateFlow<Float> = _amplitudeFlow.asStateFlow()

    private val _isRecordingFlow = MutableStateFlow(false)
    val isRecordingFlow: StateFlow<Boolean> = _isRecordingFlow.asStateFlow()

    private val _silentMicWarningFlow = MutableStateFlow(false)
    val silentMicWarningFlow: StateFlow<Boolean> = _silentMicWarningFlow.asStateFlow()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun record(context: Context): Boolean =
        withContext(ioDispatcher) {
            if (_isRecordingFlow.value) {
                Log.w(TAG, "Recording already in progress")
                return@withContext true
            }

            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "RECORD_AUDIO permission not granted")
                return@withContext false
            }

            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_ENCODING)
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size for AudioRecord")
                return@withContext false
            }

            val bufferSize = maxOf(minBufferSize * 2, 2048)

            val record =
                try {
                    AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_ENCODING,
                        bufferSize,
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to instantiate AudioRecord", e)
                    return@withContext false
                }

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord state is not STATE_INITIALIZED")
                record.release()
                return@withContext false
            }

            val voiceNotesDir = context.filesDir.resolve("voice_notes").apply { mkdirs() }
            val sessionPrefix = "note_${UUID.randomUUID()}"
            val initialFile = voiceNotesDir.resolve("${sessionPrefix}_part0.pcm")

            completedFiles.clear()
            currentActiveFile = initialFile
            _silentMicWarningFlow.value = false
            _amplitudeFlow.value = 0f

            val channel = Channel<Command>(Channel.BUFFERED)
            commandChannel = channel

            try {
                record.startRecording()
            } catch (e: Exception) {
                Log.e(TAG, "AudioRecord.startRecording failed", e)
                record.release()
                return@withContext false
            }
            audioRecord = record
            _isRecordingFlow.value = true

            try {
                coroutineScope {
                    // 1. Dedicated file-writer coroutine consuming commands sequentially
                    launch {
                        var currentOutputStream = FileOutputStream(initialFile)
                        var currentFile = initialFile

                        for (command in channel) {
                            when (command) {
                                is Command.WriteData -> {
                                    try {
                                        currentOutputStream.write(command.bytes)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed writing PCM bytes to file", e)
                                    }
                                }

                                is Command.Rotate -> {
                                    try {
                                        currentOutputStream.flush()
                                        currentOutputStream.close()
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error closing previous file on rotate", e)
                                    }
                                    completedFiles.add(currentFile)
                                    currentFile = command.newFile
                                    currentActiveFile = command.newFile
                                    currentOutputStream = FileOutputStream(currentFile)
                                    command.deferred.complete(completedFiles.last())
                                }

                                is Command.Stop -> {
                                    try {
                                        currentOutputStream.flush()
                                        currentOutputStream.close()
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error closing file on stop", e)
                                    }
                                    completedFiles.add(currentFile)
                                    currentActiveFile = null
                                    command.deferred.complete(completedFiles.toList())
                                    break
                                }

                                is Command.Cancel -> {
                                    try {
                                        currentOutputStream.flush()
                                        currentOutputStream.close()
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error closing file on cancel", e)
                                    }
                                    completedFiles.add(currentFile)
                                    currentActiveFile = null
                                    completedFiles.forEach { it.delete() }
                                    completedFiles.clear()
                                    command.deferred.complete(Unit)
                                    break
                                }
                            }
                        }
                    }

                    // 2. IO reading coroutine reading from AudioRecord
                    launch {
                        val readBuffer = ByteArray(bufferSize)
                        var silentTimeMs = 0L
                        val readIntervalMs =
                            (bufferSize.toDouble() / (SAMPLE_RATE * 2) * 1000).toLong().coerceAtLeast(10L)

                        while (isActive && _isRecordingFlow.value) {
                            val bytesRead = record.read(readBuffer, 0, readBuffer.size)
                            if (bytesRead > 0) {
                                val rms = calculateRms(readBuffer, bytesRead)
                                _amplitudeFlow.value = rms

                                if (rms <= 0.005f) {
                                    silentTimeMs += readIntervalMs
                                    if (silentTimeMs >= SILENCE_WARNING_THRESHOLD_MS) {
                                        _silentMicWarningFlow.value = true
                                    }
                                } else {
                                    silentTimeMs = 0L
                                    _silentMicWarningFlow.value = false
                                }

                                val dataCopy = readBuffer.copyOf(bytesRead)
                                try {
                                    channel.send(Command.WriteData(dataCopy))
                                } catch (_: ClosedSendChannelException) {
                                    break
                                }
                            } else if (bytesRead < 0) {
                                Log.e(TAG, "AudioRecord.read returned error code: $bytesRead")
                                break
                            }
                        }
                    }
                }
            } finally {
                _isRecordingFlow.value = false
                _amplitudeFlow.value = 0f
                _silentMicWarningFlow.value = false
                try {
                    record.stop()
                } catch (_: Exception) {
                }
                try {
                    record.release()
                } catch (_: Exception) {
                }
                if (audioRecord === record) {
                    audioRecord = null
                }
                commandChannel = null
            }

            true
        }

    suspend fun rotateSegment(context: Context): File? =
        withContext(ioDispatcher) {
            val channel = commandChannel ?: return@withContext null
            if (!_isRecordingFlow.value) return@withContext null

            val voiceNotesDir = context.filesDir.resolve("voice_notes").apply { mkdirs() }
            val nextIndex = completedFiles.size + 1
            val sessionPrefix = "note_${UUID.randomUUID()}"
            val newFile = voiceNotesDir.resolve("${sessionPrefix}_part$nextIndex.pcm")

            val deferred = CompletableDeferred<File>()
            channel.send(Command.Rotate(newFile, deferred))
            deferred.await()
        }

    suspend fun stop(): List<File> =
        withContext(ioDispatcher) {
            _isRecordingFlow.value = false
            _amplitudeFlow.value = 0f
            _silentMicWarningFlow.value = false

            val channel = commandChannel
            val result =
                if (channel != null) {
                    val deferred = CompletableDeferred<List<File>>()
                    channel.send(Command.Stop(deferred))
                    deferred.await()
                } else {
                    completedFiles.toList()
                }

            commandChannel = null
            result
        }

    suspend fun cancel() =
        withContext(ioDispatcher) {
            _isRecordingFlow.value = false
            _amplitudeFlow.value = 0f
            _silentMicWarningFlow.value = false

            val channel = commandChannel
            if (channel != null) {
                val deferred = CompletableDeferred<Unit>()
                channel.send(Command.Cancel(deferred))
                deferred.await()
            }
            commandChannel = null
        }

    companion object {
        fun calculateRms(
            buffer: ByteArray,
            bytesRead: Int,
        ): Float {
            val sampleCount = bytesRead / 2
            if (sampleCount <= 0) return 0f
            var sum = 0.0
            for (i in 0 until sampleCount) {
                val low = buffer[i * 2].toInt() and 0xFF
                val high = buffer[i * 2 + 1].toInt()
                val sample = (high shl 8) or low
                sum += sample.toDouble() * sample.toDouble()
            }
            val rms = sqrt(sum / sampleCount)
            return (rms / 32767.0).toFloat().coerceIn(0f, 1f)
        }
    }
}
