package com.ramitsuri.notificationjournal.ai.extractor

import android.content.Context
import android.os.Build
import com.google.ai.edge.litertlm.Backend
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import java.io.File

object IntentExtractorFactory {
    fun createAddEntryExtractor(
        context: Context? = null,
        prefManager: PrefManager? = null,
        modelFile: File? = null,
        backend: Backend = Backend.CPU(),
    ): AddEntryIntentExtractor {
        val resolvedModelFile = modelFile ?: context?.let { CustomModelHelper.getModelFile(it) }

        if (resolvedModelFile != null &&
            resolvedModelFile.exists() &&
            resolvedModelFile.isFile &&
            resolvedModelFile.length() > 0
        ) {
            return LiteRtLmAddEntryIntentExtractor(
                modelFile = resolvedModelFile,
                backend = backend,
                cacheDir = context?.cacheDir,
                prefManager = prefManager,
                fallbackExtractor = createFallbackExtractor(prefManager),
            )
        }

        return createFallbackExtractor(prefManager)
    }

    private fun createFallbackExtractor(prefManager: PrefManager? = null): AddEntryIntentExtractor {
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
