package com.ramitsuri.notificationjournal.ai

import com.ramitsuri.notificationjournal.ai.voice.PcmAudioRecorder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmAudioRecorderTest {
    @Test
    fun testRmsCalculation_silence() {
        val buffer = ByteArray(1024) { 0 }
        val rms = PcmAudioRecorder.calculateRms(buffer, buffer.size)
        assertEquals(0f, rms, 0.0001f)
    }

    @Test
    fun testRmsCalculation_fullScaleSine() {
        // Full scale 16-bit max amplitude
        val buffer = ByteArray(4)
        // Sample 1: 32767
        buffer[0] = 0xFF.toByte()
        buffer[1] = 0x7F.toByte()
        // Sample 2: 32767
        buffer[2] = 0xFF.toByte()
        buffer[3] = 0x7F.toByte()

        val rms = PcmAudioRecorder.calculateRms(buffer, buffer.size)
        assertTrue(rms > 0.99f)
    }
}
