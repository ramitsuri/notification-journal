package com.ramitsuri.notificationjournal.ai.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

private const val TAG = "PcmAudioPlayer"

class PcmAudioPlayer(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private var audioTrack: AudioTrack? = null

    private val _isPlayingFlow = MutableStateFlow(false)
    val isPlayingFlow: StateFlow<Boolean> = _isPlayingFlow.asStateFlow()

    suspend fun playPcm(file: File) =
        withContext(ioDispatcher) {
            stop()
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: ${file.absolutePath}")
                return@withContext
            }

            val bufferSize =
                AudioTrack
                    .getMinBufferSize(
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AUDIO_ENCODING,
                    ).coerceAtLeast(2048)

            val track =
                try {
                    AudioTrack
                        .Builder()
                        .setAudioAttributes(
                            AudioAttributes
                                .Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build(),
                        ).setAudioFormat(
                            AudioFormat
                                .Builder()
                                .setEncoding(AUDIO_ENCODING)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build(),
                        ).setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create AudioTrack", e)
                    return@withContext
                }

            audioTrack = track
            _isPlayingFlow.value = true

            try {
                track.play()
                FileInputStream(file).use { input ->
                    val buffer = ByteArray(bufferSize)
                    var read = 0
                    while (isActive && _isPlayingFlow.value && input.read(buffer).also { read = it } > 0) {
                        track.write(buffer, 0, read)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during AudioTrack playback", e)
            } finally {
                _isPlayingFlow.value = false
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {
                }
                if (audioTrack === track) {
                    audioTrack = null
                }
            }
        }

    fun stop() {
        _isPlayingFlow.value = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
    }
}
