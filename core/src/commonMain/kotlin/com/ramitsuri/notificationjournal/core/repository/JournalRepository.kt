package com.ramitsuri.notificationjournal.core.repository

import com.ramitsuri.notificationjournal.core.data.JournalEntryDao
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

class JournalRepository(
    private val dao: JournalEntryDao,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    fun getFlow(): Flow<List<JournalEntry>> {
        return dao.getAllFlow().map { list ->
            list.sortedBy { it.entryTime }
        }
    }

    suspend fun getAll() = dao.getAll()

    suspend fun get(id: String): JournalEntry {
        return dao.get(id)
    }

    suspend fun update(journalEntry: JournalEntry) {
        dao.update(journalEntry)
    }

    suspend fun insert(text: String, tag: String? = null, originalEntryTime: Instant? = null) {
        val entryTime = clock.now()
        text
            .split("\n")
            .filter { it.isNotBlank() }
            .forEach {
                insert(
                    JournalEntry(
                        entryTime = entryTime,
                        timeZone = timeZone,
                        text = it.trim(),
                        tag = tag,
                        entryTimeOverride = originalEntryTime
                    )
                )
            }
    }

    suspend fun insert(entry: JournalEntry) {
        dao.insert(entry)
    }

    suspend fun delete(entry: JournalEntry) {
        dao.delete(listOf(entry))
    }

    suspend fun upload(): String? {
        val entries = dao.getAll()
        if (entries.isEmpty()) {
            return null
        }
        // TODO share with app clients
        return "Not supported"
        /*val response = api.sendData(entries.toDayGroups())
        return if (response == null) {
            "Null response"
        } else if (response.status == HttpStatusCode.OK) {
            dao.updateUploaded(entries)
            null
        } else {
            "Message: ${response.bodyAsText()}, Code: ${response.status}"
        }*/
    }
}