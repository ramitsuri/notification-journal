package com.ramitsuri.notificationjournal.ai.extractor

import android.content.Context
import java.io.File

object CustomModelHelper {
    /**
     * Resolves the custom .litertlm model file.
     * Searches in order:
     * 1. customPath (if provided and valid)
     * 2. context.getExternalFilesDir(null)/ (Android/data/com.ramitsuri.notificationjournal/files/)
     * 3. context.filesDir/
     */
    fun getModelFile(
        context: Context,
        customPath: String? = null,
    ): File? {
        if (!customPath.isNullOrBlank()) {
            val file = File(customPath)
            if (file.exists() && file.isFile && file.length() > 0) {
                return file
            }
        }

        // 1. Check external files dir (Android/data/<package>/files/)
        val extDir = context.getExternalFilesDir(null)
        if (extDir != null) {
            findInDirectory(extDir)?.let { return it }
        }

        // 2. Check internal files dir
        findInDirectory(context.filesDir)?.let { return it }

        return null
    }

    private fun findInDirectory(dir: File?): File? {
        if (dir == null || !dir.exists() || !dir.isDirectory || !dir.canRead()) {
            return null
        }
        for (model in SupportedCustomModel.entries) {
            val file = File(dir, model.fileName)
            if (file.exists() && file.isFile && file.canRead() && file.length() > 0) {
                return file
            }
        }
        return null
    }
}
