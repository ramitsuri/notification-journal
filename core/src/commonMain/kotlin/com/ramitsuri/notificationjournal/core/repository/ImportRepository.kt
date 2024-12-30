package com.ramitsuri.notificationjournal.core.repository

import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface ImportRepository {
    val importedEntriesFlow: Flow<List<JournalEntry>>

    suspend fun import(fromDir: String, startDate: LocalDate, endDate: LocalDate)
}
