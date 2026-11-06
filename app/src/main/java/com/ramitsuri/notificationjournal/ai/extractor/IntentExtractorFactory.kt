package com.ramitsuri.notificationjournal.ai.extractor

import android.os.Build
import com.ramitsuri.notificationjournal.core.utils.PrefManager

object IntentExtractorFactory {
    fun createAddEntryExtractor(prefManager: PrefManager? = null): AddEntryIntentExtractor {
        val isEmulator =
            Build.FINGERPRINT.startsWith("generic") ||
                Build.MODEL.contains("google_sdk") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu")

        return if (isEmulator) {
            RuleBasedAddEntryIntentExtractor()
        } else {
            NanoAddEntryIntentExtractor(prefManager = prefManager)
        }
    }
}
