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
    fun getFlow(
        showReconciled: Boolean = false,
    ): Flow<List<JournalEntry>> {
        return if (showReconciled) {
            dao.getAllFlow()
        } else {
            dao.getAllFlowNotReconciled()
        }
    }

    fun getForUploadCountFlow() = dao.getForUploadCountFlow()

    suspend fun getAll() = dao.getAll()

    suspend fun get(id: String): JournalEntry {
        return dao.get(id)
    }

    suspend fun update(journalEntry: JournalEntry) {
        dao.update(journalEntry.copy(uploaded = false))
    }

    suspend fun insert(
        text: String,
        tag: String? = null,
        time: Instant = clock.now(),
        originalEntryTime: Instant? = null,
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
                )
            }
    }

    suspend fun insert(
        entry: JournalEntry,
    ) {
        dao.insert(entry)
    }

    suspend fun delete(entry: JournalEntry) {
        dao.update(entry.copy(deleted = true, uploaded = false))
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
            dao.updateUploaded(entries = entries, uploaded = sent)
        }
    }
}