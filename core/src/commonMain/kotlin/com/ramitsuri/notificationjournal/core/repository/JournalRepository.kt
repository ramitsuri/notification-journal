package com.ramitsuri.notificationjournal.core.repository

import com.ramitsuri.notificationjournal.core.data.JournalEntryDao
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import com.ramitsuri.notificationjournal.core.network.DataSendHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.milliseconds

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

    fun getForUploadCountFlow() = dao.getForUploadCountFlow()

    suspend fun getAll() = dao.getAll()

    suspend fun get(id: String): JournalEntry {
        return dao.get(id)
    }

    suspend fun update(journalEntry: JournalEntry) {
        val updated = dao.update(journalEntry)
        sendAndMarkUploaded(listOf(updated))
    }

    suspend fun insert(
        text: String,
        tag: String? = null,
        time: Instant = clock.now(),
        originalEntryTime: Instant? = null,
        send: Boolean = true,
    ) {
        text
            .split("\n")
            .filter { it.isNotBlank() }
            .forEachIndexed { index, entry ->
                val entryTime = time.plus(index.times(10).milliseconds)
                insert(
                    entry = JournalEntry(
                        entryTime = entryTime,
                        timeZone = timeZone,
                        text = entry.trim(),
                        tag = tag,
                        entryTimeOverride = originalEntryTime
                    ),
                    send = send,
                )
            }
    }

    suspend fun insert(
        entry: JournalEntry,
        send: Boolean = true,
    ) {
        val inserted = dao.insert(entry)
        if (send) {
            sendAndMarkUploaded(listOf(inserted))
        }
    }

    suspend fun delete(entry: JournalEntry) {
        val deleted = dao.update(entry.copy(deleted = true))
        sendAndMarkUploaded(listOf(deleted))
    }

    suspend fun sync() {
        val entries = dao.getForUpload()
        if (entries.isEmpty()) {
            return
        }
        sendAndMarkUploaded(entries)
    }

    suspend fun handlePayload(payload: Payload.Entries) {
        payload.data.forEach {
            // Since they're coming from a different client, they should be considered
            // uploaded for this client so that we don't upload them again.
            dao.upsert(it.copy(uploaded = true))
        }
    }

    private fun sendAndMarkUploaded(entries: List<JournalEntry>) {
        coroutineScope.launch {
            val sent = dataSendHelper?.sendEntry(entries) == true
            if (sent) {
                dao.updateUploaded(entries)
            }
        }
    }
}