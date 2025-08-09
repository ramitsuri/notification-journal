package com.ramitsuri.notificationjournal.core.repository

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.data.EntryConflictDao
import com.ramitsuri.notificationjournal.core.data.JournalEntryDao
import com.ramitsuri.notificationjournal.core.model.DateWithCount
import com.ramitsuri.notificationjournal.core.model.EntryConflict
import com.ramitsuri.notificationjournal.core.model.SortOrder
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.stats.EntryStats
import com.ramitsuri.notificationjournal.core.model.sync.Entity
import com.ramitsuri.notificationjournal.core.model.sync.Sender
import com.ramitsuri.notificationjournal.core.model.sync.VerifyEntries
import com.ramitsuri.notificationjournal.core.network.DataSendHelper
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import com.ramitsuri.notificationjournal.core.utils.hourMinute
import com.ramitsuri.notificationjournal.core.utils.nowLocal
import com.ramitsuri.notificationjournal.core.utils.plus
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.am
import notificationjournal.core.generated.resources.pm
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration.Companion.milliseconds

class JournalRepository(
    private val dao: JournalEntryDao,
    private val conflictDao: EntryConflictDao,
    private val clock: Clock = Clock.System,
    private val dataSendHelper: DataSendHelper,
    private val prefManager: PrefManager,
) {
    fun getFlow(): Flow<List<JournalEntry>> {
        return dao.getAllFlowNotReconciled()
    }

    fun getNotReconciledEntryDatesFlow(): Flow<List<DateWithCount>> {
        return combine(
            // Used so that it refreshes the data
            conflictDao.getFlow(),
            dao.getNotReconciledEntryTimesFlow(),
        ) { _, entryTimes ->
            entryTimes.map { it.date }.distinct()
        }.map { entryDates ->
            entryDates.map { date ->
                val entriesForDate = getForDateFlow(date).first()
                val conflictCount = entriesForDate.map { it.id }.let { conflictDao.getCount(it) }
                val untaggedCount = entriesForDate.count { Tag.isNoTag(it.tag) }
                DateWithCount(date = date, conflictCount = conflictCount, untaggedCount = untaggedCount)
            }
        }
    }

    fun getForDateFlow(date: LocalDate): Flow<List<JournalEntry>> {
        return dao.getForDateFlow(date.toString())
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
        uploadEntries: Boolean,
    ) {
        val uploaded =
            if (uploadEntries) {
                // If not able to send it right away, these entries would need to be reimported for them to show up
                // on other devices. Otherwise, need to add support for tracking which days have been imported.
                dataSendHelper.sendClearDaysAndInsertEntries(days, entries) == true
            } else {
                // If uploaded is false, then we're receiving these entries from another device, consider them uploaded
                // because other device already has them.
                true
            }
        dao.clearDaysAndInsert(days.map { it.toString() }, entries.map { it.copy(uploaded = uploaded) })
    }

    suspend fun insert(
        text: String,
        tag: String? = null,
        time: LocalDateTime = clock.nowLocal(),
    ) {
        val defaultTag = prefManager.getDefaultTag()
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
                JournalEntry(entryTime = entryTime, text = entryText, tag = tag ?: defaultTag)
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

    suspend fun uploadAll() {
        Logger.i(TAG) { "Attempting to upload all" }
        val entries = dao.getForUpload()
        if (entries.isEmpty()) {
            Logger.i(TAG) { "Nothing to upload all" }
            return
        }
        upload(entries)
        dao.deleteDeleted()
    }

    suspend fun upload(entries: List<JournalEntry>) {
        Logger.i(TAG) { "Syncing ${entries.size} entries" }
        entries.chunked(10).forEach {
            sendAndMarkUploaded(it)
        }
    }

    suspend fun handlePayload(payload: Entity.Entries) {
        payload.data.forEach { payloadEntry ->
            // Since they're coming from a different client, they should be considered
            // uploaded for this client so that we don't upload them again.

            // Doesn't exist locally or sender asked to replace local entry
            val existing = dao.get(payloadEntry.id)
            if (existing == null || payloadEntry.replacesLocal) {
                // Save with replacesLocal false because that was a one time thing, we don't want this entry to be sent
                // with replacesLocal anymore
                dao.upsert(payloadEntry.copy(uploaded = true, replacesLocal = false))
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
        return conflictDao.getFlow()
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
        sendAndMarkUploaded(listOf(newEntry.copy(replacesLocal = true)))
    }

    suspend fun search(
        query: String,
        tags: List<String>?,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        exactMatch: Boolean = false,
        sortOrder: SortOrder = SortOrder.DESC,
    ): List<JournalEntry> {
        val startDateString = startDate?.toString()
        val endDateString = endDate?.toString()
        val sortAscending = sortOrder == SortOrder.DESC
        return if (tags == null) {
            dao.search(
                query = query,
                startDate = startDateString,
                endDate = endDateString,
                exactMatch = exactMatch,
                sortAscending = sortAscending,
            )
        } else {
            dao.search(
                query = query,
                tags = tags,
                startDate = startDateString,
                endDate = endDateString,
                exactMatch = exactMatch,
                sortAscending = sortAscending,
            )
        }
    }

    fun getEntryTags(): Flow<List<String>> {
        return dao.getEntryTags()
    }

    suspend fun markAllReconciled() {
        dao.markAllReconciled()
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

    suspend fun getVerificationForDate(date: LocalDate): VerifyEntries.Verification? {
        return getForDateFlow(date)
            .firstOrNull()
            ?.let {
                VerifyEntries.Verification(entries = it)
            }
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

    private suspend fun sendAndMarkUploaded(entries: List<JournalEntry>) {
        val sent = dataSendHelper.sendEntries(entries) == true
        if (sent) {
            // Once sent, mark as not "replaces_local" because that was just for this upload, further
            // changes should go through the conflict resolution flow
            dao.updateUploaded(entries = entries.map { it.copy(replacesLocal = false) }, uploaded = true)
        } else {
            dao.updateUploaded(entries = entries, uploaded = false)
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
        if (tag == Tag.NO_TAG.value &&
            incomingEntry.tag != Tag.NO_TAG.value
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
