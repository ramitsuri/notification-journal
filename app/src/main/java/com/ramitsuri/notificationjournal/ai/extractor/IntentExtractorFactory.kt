package com.ramitsuri.notificationjournal.ai.extractor

import android.os.Build

object IntentExtractorFactory {
    fun create(): JournalIntentExtractor {
        val isEmulator =
            Build.FINGERPRINT.startsWith("generic") ||
                Build.MODEL.contains("google_sdk") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu")

        return if (isEmulator) {
            RuleBasedJournalIntentExtractor()
        } else {
            NanoJournalIntentExtractor()
        }
    }
}
