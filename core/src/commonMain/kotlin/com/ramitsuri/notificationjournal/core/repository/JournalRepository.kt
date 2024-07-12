package com.ramitsuri.notificationjournal.core.repository

import com.ramitsuri.notificationjournal.core.data.JournalEntryDao
import com.ramitsuri.notificationjournal.core.model.entry.EntriesVerification
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import com.ramitsuri.notificationjournal.core.model.sync.Sender
import com.ramitsuri.notificationjournal.core.network.DataSendHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    private val _entriesVerification = MutableStateFlow(EntriesVerification())
    val entriesVerification = _entriesVerification.asStateFlow()

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
            dao.updateUploaded(entries = entries, uploaded = sent)
        }
    }

    suspend fun sendForVerification(entries: List<JournalEntry>) {
        val sent = dataSendHelper?.sendVerifyEntries(entries) == true
        if (sent) {
            _entriesVerification.update {
                EntriesVerification().copy(sentEntries = entries)
            }
        }
    }

    suspend fun onVerifyEntriesReceived(
        entries: List<JournalEntry>,
        sender: Sender,
    ) {
        if (_entriesVerification.value.sentEntries.isEmpty()) {
            // Verification initiated by another client

            // Insert missing ones and then respond with
            // what we have so that other device can complete verification
            dao.insertOrIgnoreIfPresent(entries)
            dataSendHelper?.sendVerifyEntries(dao.getAll(entries.map { it.id }))
        } else {
            // Verification initiated by this client

            // Verification already in progress as we've received some entries
            if (_entriesVerification.value.receivedEntries.isNotEmpty()) {
                return
            }
            // Store received entries to complete verification
            _entriesVerification.update {
                it.copy(receivedEntries = entries, verifiedBy = sender)
            }
        }
    }

    fun resetVerification() {
        _entriesVerification.update {
            EntriesVerification()
        }
    }
}