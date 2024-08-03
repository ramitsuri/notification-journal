package com.ramitsuri.notificationjournal.core.repository

import com.ramitsuri.notificationjournal.core.data.JournalEntryDao
import com.ramitsuri.notificationjournal.core.model.entry.EntriesVerification
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import com.ramitsuri.notificationjournal.core.model.sync.Sender
import com.ramitsuri.notificationjournal.core.network.DataSendHelper
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.formatForDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.am
import notificationjournal.core.generated.resources.pm
import org.jetbrains.compose.resources.getString
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
        dao.update(journalEntry.copy(uploaded = false))
    }

    suspend fun insert(
        text: String,
        tag: String? = null,
        time: Instant = clock.now(),
        timeZone: TimeZone = this.timeZone,
    ) {
        text
            .split("\n")
            .filter { it.isNotBlank() }
            .forEachIndexed { index, entry ->
                val entryTime = time.plus(index.times(10).milliseconds)
                val entryText = if (entry.contains(Constants.TEMPLATED_TIME)) {
                    entry.replace(
                        Constants.TEMPLATED_TIME,
                        formatForDisplay(
                            toFormat = entryTime,
                            timeZone = timeZone,
                            amString = getString(Res.string.am),
                            pmString = getString(Res.string.pm),
                        )
                    )
                } else {
                    entry
                }
                dao.insert(
                    entry = JournalEntry(
                        entryTime = time,
                        timeZone = timeZone,
                        text = entryText.trim(),
                        tag = tag,
                        entryTimeOverride = entryTime,
                    ),
                )
            }
    }

    suspend fun delete(entry: JournalEntry) {
        dao.update(entry.copy(deleted = true, uploaded = false))
    }

    suspend fun sync() {
        val entries = dao.getForUpload()
        if (entries.isEmpty()) {
            return
        }
        entries.chunked(10).forEach {
            sendAndMarkUploaded(it)
        }
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