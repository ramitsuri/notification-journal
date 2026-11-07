package com.ramitsuri.notificationjournal.core

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.repository.ExportRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import java.nio.file.Files
import java.nio.file.Paths

class ExportRepositoryImpl(
    private val ioDispatcher: CoroutineDispatcher,
) : ExportRepository {
    override suspend fun export(
        baseDir: String,
        forDate: LocalDate,
        content: String,
    ) {
        if (baseDir.isBlank()) {
            Logger.withTag(TAG).w { "Export base directory is blank" }
            return
        }
        val year = forDate.year.toString()
        val month = forDate.monthNumber.toString().padStart(2, '0')
        val day = forDate.dayOfMonth.toString().padStart(2, '0')
        withContext(ioDispatcher) {
            try {
                val file = Paths.get(baseDir, year, month, "$day.md")
                Files.createDirectories(file.parent)
                Files.writeString(file, content)
                Logger.withTag(TAG).i { "Exported entries for $forDate to $file" }
            } catch (e: Exception) {
                Logger.withTag(TAG).e(e) { "Failed to export entries for $forDate" }
                throw e
            }
        }
    }

    companion object {
        private const val TAG = "ExportRepository"
    }
}
