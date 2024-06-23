package com.ramitsuri.notificationjournal.core.repository

import com.ramitsuri.notificationjournal.core.data.JournalEntryDao
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Action
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import com.ramitsuri.notificationjournal.core.network.DataSendHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

class JournalRepository(
    private val coroutineScope: CoroutineScope,
    private val dao: JournalEntryDao,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    private val dataSendHelper: DataSendHelper?,
) {
    fun getFlow(): Flow<List<JournalEntry>> {
        return dao.getAllFlow()
    }

    suspend fun getAll() = dao.getAll()

    suspend fun get(id: String): JournalEntry {
        return dao.get(id)
    }

    suspend fun update(journalEntry: JournalEntry) {
        val updated = dao.update(journalEntry)
        sendAndMarkUploaded(updated, Action.UPDATE)
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
        val inserted = dao.insert(entry)
        sendAndMarkUploaded(inserted, Action.CREATE)
    }

    suspend fun delete(entry: JournalEntry) {
        val deleted = markAsDeleted(entry)
        sendAndMarkUploaded(deleted, Action.DELETE)
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

    suspend fun handlePayload(payload: Payload.Entries) {
        when (payload.action) {
            Action.CREATE,
            Action.UPDATE -> {
                payload.data.forEach {
                    // Since they're coming from a different client, they should be considered
                    // uploaded for this client so that we don't upload them again.
                    dao.upsert(it.copy(uploaded = true))
                }
            }

            Action.DELETE -> payload.data.forEach { markAsDeleted(it) }
        }
    }

    private suspend fun markAsUploaded(entry: JournalEntry) {
        dao.updateUploaded(listOf(entry))
    }

    private suspend fun markAsDeleted(entry: JournalEntry): JournalEntry {
        return dao.update(entry.copy(deleted = true))
    }

    private fun sendAndMarkUploaded(entry: JournalEntry, action: Action) {
        coroutineScope.launch {
            val sent = dataSendHelper?.sendEntry(entry, action) == true
            if (sent) {
                markAsUploaded(entry)
            }
        }
    }
}