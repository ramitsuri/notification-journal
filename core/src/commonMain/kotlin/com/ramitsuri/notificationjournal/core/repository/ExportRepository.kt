package com.ramitsuri.notificationjournal.core.repository

import kotlinx.datetime.LocalDate

interface ExportRepository {
    suspend fun export(
        baseDir: String,
        forDate: LocalDate,
        content: String,
    )
}
