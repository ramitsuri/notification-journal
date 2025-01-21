package com.ramitsuri.notificationjournal.core.repository

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.data.EntryConflictDao
import com.ramitsuri.notificationjournal.core.data.JournalEntryDao
import com.ramitsuri.notificationjournal.core.model.EntryConflict
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.stats.EntryStats
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import com.ramitsuri.notificationjournal.core.model.sync.Sender
import com.ramitsuri.notificationjournal.core.network.DataSendHelper
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.hourMinute
import com.ramitsuri.notificationjournal.core.utils.nowLocal
import com.ramitsuri.notificationjournal.core.utils.plus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.am
import notificationjournal.core.generated.resources.pm
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration.Companion.milliseconds

class JournalRepository(
    private val coroutineScope: CoroutineScope,
    private val dao: JournalEntryDao,
    private val conflictDao: EntryConflictDao,
    private val clock: Clock = Clock.System,
    private val dataSendHelper: DataSendHelper?,
) {
    fun getFlow(showReconciled: Boolean = false): Flow<List<JournalEntry>> {
        return if (showReconciled) {
            dao.getAllFlow()
        } else {
            dao.getAllFlowNotReconciled()
        }
    }

    fun getForUploadCountFlow() = dao.getForUploadCountFlow()

    suspend fun getAll() = dao.getAll()

    suspend fun get(id: String): JournalEntry? {
        return dao.get(id)
    }

    suspend fun update(journalEntry: JournalEntry) {
        update(listOf(journalEntry))
    }

    // Separate so that not all calls have to check if text contains template time
    suspend fun updateText(journalEntry: JournalEntry) {
        update(
            listOf(
                journalEntry.copy(
                    text =
                        replaceWithTimeTemplateIfNecessary(
                            originalText = journalEntry.text,
                            time = journalEntry.entryTime,
                        ),
                ),
            ),
        )
    }

    suspend fun update(journalEntries: List<JournalEntry>) {
        dao.update(journalEntries.map { it.copy(uploaded = false) })
    }

    suspend fun clearDaysAndInsert(
        days: List<LocalDate>,
        entries: List<JournalEntry>,
    ) {
        dao.clearDaysAndInsert(days.map { it.toString() }, entries)
    }

    suspend fun insert(
        text: String,
        tag: String? = null,
        time: LocalDateTime = clock.nowLocal(),
    ) {
        text
            .split("\n")
            .filter { it.isNotBlank() }
            .mapIndexed { index, entry ->
                val entryTime = time.plus(index.times(10).milliseconds)
                val entryText =
                    replaceWithTimeTemplateIfNecessary(
                        originalText = entry,
                        time = entryTime,
                    ).trim()
                JournalEntry(entryTime = entryTime, text = entryText, tag = tag ?: Tag.NO_TAG.value)
            }
            .let {
                dao.insert(it)
            }
    }

    suspend fun delete(entry: JournalEntry) {
        delete(listOf(entry))
    }

    suspend fun delete(entries: List<JournalEntry>) {
        update(entries.map { it.copy(deleted = true) })
    }

    suspend fun sync() {
        Logger.i(TAG) { "Attempting to sync" }
        val entries = dao.getForUpload()
        if (entries.isEmpty()) {
            Logger.i(TAG) { "Nothing to sync" }
            return
        }
        upload(entries)
    }

    fun upload(entries: List<JournalEntry>) {
        Logger.i(TAG) { "Syncing ${entries.size} entries" }
        entries.chunked(10).forEach {
            sendAndMarkUploaded(it)
        }
    }

    suspend fun handlePayload(payload: Payload.Entries) {
        payload.data.forEach { payloadEntry ->
            // Since they're coming from a different client, they should be considered
            // uploaded for this client so that we don't upload them again.

            // Doesn't exist locally or sender asked to replace local entry
            val existing = dao.get(payloadEntry.id)
            if (existing == null || payload.replacesLocal) {
                dao.upsert(payloadEntry.copy(uploaded = true))
                return@forEach
            }

            // No conflict between entries, save received entry
            val entryConflict = existing.getEntryConflict(payloadEntry, payload.sender)
            if (entryConflict == null) {
                dao.upsert(payloadEntry.copy(uploaded = true))
                return@forEach
            }

            // Conflict between local and received entries, leave saved entry alone, save conflict
            conflictDao.insert(entryConflict)
        }
    }

    fun getConflicts(): Flow<List<EntryConflict>> {
        return conflictDao.getAllFlow()
    }

    suspend fun resolveConflict(
        entry: JournalEntry,
        selectedConflict: EntryConflict?,
    ) {
        val newEntry =
            if (selectedConflict == null) { // No conflict selected, retain local entry
                entry
            } else {
                entry.copy(
                    text = selectedConflict.text,
                    entryTime = selectedConflict.entryTime,
                    tag = selectedConflict.tag,
                )
            }

        if (selectedConflict != null) {
            update(newEntry)
        }

        conflictDao.deleteForEntryId(newEntry.id)
        sendAndMarkUploaded(listOf(newEntry), replacesLocal = true)
    }

    suspend fun search(
        query: String,
        tags: List<String>?,
    ): List<JournalEntry> {
        return if (tags == null) {
            dao.search(query)
        } else {
            dao.search(query, tags)
        }
    }

    fun getEntryTags(): Flow<List<String>> {
        return dao.getEntryTags()
    }

    suspend fun getStats(): EntryStats =
        coroutineScope {
            fun List<LocalDateTime>.dates() =
                map { it.date }
                    .distinct()
                    .size
                    .toString()

            val notUploadedNotReconciled =
                let {
                    val uploaded = false
                    val reconciled = false
                    async { dao.getEntryTimes(uploaded, reconciled).dates() } to
                        async { dao.getEntryCount(uploaded, reconciled).toString() }
                }
            val uploadedNotReconciled =
                let {
                    val uploaded = true
                    val reconciled = false
                    async { dao.getEntryTimes(uploaded, reconciled).dates() } to
                        async { dao.getEntryCount(uploaded, reconciled).toString() }
                }
            val notUploadedReconciled =
                let {
                    val uploaded = false
                    val reconciled = true
                    async { dao.getEntryTimes(uploaded, reconciled).dates() } to
                        async { dao.getEntryCount(uploaded, reconciled).toString() }
                }
            val uploadedReconciled =
                let {
                    val uploaded = true
                    val reconciled = true
                    async { dao.getEntryTimes(uploaded, reconciled).dates() } to
                        async { dao.getEntryCount(uploaded, reconciled).toString() }
                }
            // All dates won't necessarily add up to individual dates because same date could have
            // uploaded as well as not uploaded entries for example
            val all =
                let {
                    async { dao.getEntryTimes().dates() } to
                        async { dao.getEntryCount().toString() }
                }
            EntryStats(
                uploadedAndReconciled =
                    EntryStats.Count(
                        days = uploadedReconciled.first.await(),
                        entries = uploadedReconciled.second.await(),
                    ),
                uploadedAndNotReconciled =
                    EntryStats.Count(
                        days = uploadedNotReconciled.first.await(),
                        entries = uploadedNotReconciled.second.await(),
                    ),
                notUploadedAndReconciled =
                    EntryStats.Count(
                        days = notUploadedReconciled.first.await(),
                        entries = notUploadedReconciled.second.await(),
                    ),
                notUploadedAndNotReconciled =
                    EntryStats.Count(
                        days = notUploadedNotReconciled.first.await(),
                        entries = notUploadedNotReconciled.second.await(),
                    ),
                all =
                    EntryStats.Count(
                        days = all.first.await(),
                        entries = all.second.await(),
                    ),
            )
        }

    private suspend fun replaceWithTimeTemplateIfNecessary(
        originalText: String,
        time: LocalDateTime,
    ): String {
        return if (originalText.contains(Constants.TEMPLATED_TIME)) {
            originalText.replace(
                Constants.TEMPLATED_TIME,
                hourMinute(
                    toFormat = time,
                    amString = getString(Res.string.am),
                    pmString = getString(Res.string.pm),
                ),
            )
        } else {
            originalText
        }
    }

    private fun sendAndMarkUploaded(
        entries: List<JournalEntry>,
        replacesLocal: Boolean = false,
    ) {
        coroutineScope.launch {
            val sent = dataSendHelper?.sendEntry(entries, replacesLocal) == true
            dao.updateUploaded(entries = entries, uploaded = sent)
        }
    }

    private fun JournalEntry.getEntryConflict(
        incomingEntry: JournalEntry,
        incomingEntrySender: Sender,
    ): EntryConflict? {
        if (entryTime == incomingEntry.entryTime &&
            text == incomingEntry.text &&
            tag == incomingEntry.tag
        ) {
            return null
        }

        // Assume that local entry is older because the other entry now has a tag
        if ((tag == null || tag == Tag.NO_TAG.value) &&
            !(incomingEntry.tag == null || incomingEntry.tag == Tag.NO_TAG.value)
        ) {
            return null
        }

        return EntryConflict(
            entryId = incomingEntry.id,
            entryTime = incomingEntry.entryTime,
            text = incomingEntry.text,
            tag = incomingEntry.tag,
            senderName = incomingEntrySender.name,
        )
    }

    companion object {
        private const val TAG = "JournalRepository"
    }
}
