package com.ramitsuri.notificationjournal.core.ui.journalentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.EntryConflict
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.TagGroup
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.toDayGroups
import com.ramitsuri.notificationjournal.core.network.WebSocketHelper
import com.ramitsuri.notificationjournal.core.repository.ExportRepository
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import com.ramitsuri.notificationjournal.core.utils.combine
import com.ramitsuri.notificationjournal.core.utils.dayMonthDateWithYearSuspend
import com.ramitsuri.notificationjournal.core.utils.minus
import com.ramitsuri.notificationjournal.core.utils.plus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

class JournalEntryViewModel(
    selectedDate: LocalDate,
    private val repository: JournalRepository,
    private val exportRepository: ExportRepository?,
    private val tagsDao: TagsDao,
    private val prefManager: PrefManager,
    private val allowNotify: Boolean,
    webSocketHelper: WebSocketHelper,
) : ViewModel() {
    private val contentForCopy: MutableStateFlow<String> = MutableStateFlow("")

    val state: StateFlow<ViewState> =
        combine(
            contentForCopy,
            repository.getForDateFlow(selectedDate),
            repository.getForUploadCountFlow(),
            repository.getConflicts(),
            prefManager.showEmptyTags(),
            prefManager.showConflictDiffInline(),
            webSocketHelper.isConnected,
        ) {
                contentForCopy,
                entries,
                forUploadCount,
                entryConflicts,
                showEmptyTags,
                showConflictDiffInline,
                isConnected,
            ->
            val tags = tagsDao.getAll()
            val entryIds = entries.map { it.id }
            ViewState(
                dayGroup = entries.toDayGroups(tags.map { it.value }).first(),
                tags = tags,
                notUploadedCount = forUploadCount,
                entryConflicts = entryConflicts.filter { entryIds.contains(it.entryId) },
                showConflictDiffInline = showConflictDiffInline,
                contentForCopy = contentForCopy,
                showEmptyTags = showEmptyTags,
                allowNotify = allowNotify,
                isConnected = isConnected,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ViewState(dayGroup = null),
        )

    fun delete(journalEntry: JournalEntry) {
        viewModelScope.launch {
            repository.delete(journalEntry)
        }
    }

    fun delete(tagGroup: TagGroup) {
        viewModelScope.launch {
            repository.delete(tagGroup.entries)
        }
    }

    fun editTag(
        journalEntry: JournalEntry,
        tag: String,
    ) {
        if (journalEntry.tag == tag) {
            return
        }
        viewModelScope.launch {
            repository.update(journalEntry.copy(tag = tag))
        }
    }

    fun moveToPreviousDay(journalEntry: JournalEntry) {
        val currentEntryTime = journalEntry.entryTime
        val previousDayTime = currentEntryTime.minus(1.days)
        setDate(journalEntry, previousDayTime)
    }

    fun moveToNextDay(journalEntry: JournalEntry) {
        val currentEntryTime = journalEntry.entryTime
        val nextDayTime = currentEntryTime.plus(1.days)
        setDate(journalEntry, nextDayTime)
    }

    // Move it down in the list of entries that are ordered by ascending of display time
    fun moveDown(
        journalEntry: JournalEntry,
        tagGroup: TagGroup,
    ) {
        val indexOfEntry = tagGroup.entries.indexOf(journalEntry)
        if (indexOfEntry == -1 || indexOfEntry == tagGroup.entries.lastIndex) {
            return
        }
        val nextEntry = tagGroup.entries[indexOfEntry + 1]
        val newDateTime = nextEntry.entryTime.plus(1.milliseconds)
        setDate(journalEntry, newDateTime)
    }

    // Move it to bottom of the list of entries that are ordered by ascending of display time
    fun moveToBottom(
        journalEntry: JournalEntry,
        tagGroup: TagGroup,
    ) {
        val indexOfEntry = tagGroup.entries.indexOf(journalEntry)
        if (indexOfEntry == -1 || indexOfEntry == tagGroup.entries.lastIndex) {
            return
        }
        val lastEntry = tagGroup.entries.last()
        val newDateTime = lastEntry.entryTime.plus(1.milliseconds)
        setDate(journalEntry, newDateTime)
    }

    // Move it up in the list of entries that are ordered by ascending of display time
    fun moveUp(
        journalEntry: JournalEntry,
        tagGroup: TagGroup,
    ) {
        val indexOfEntry = tagGroup.entries.indexOf(journalEntry)
        if (indexOfEntry == -1 || indexOfEntry == 0) {
            return
        }
        val previousEntry = tagGroup.entries[indexOfEntry - 1]
        val newDateTime = previousEntry.entryTime.minus(1.milliseconds)
        setDate(journalEntry, newDateTime)
    }

    // Move it to top of the list of entries that are ordered by ascending of display time
    fun moveToTop(
        journalEntry: JournalEntry,
        tagGroup: TagGroup,
    ) {
        val indexOfEntry = tagGroup.entries.indexOf(journalEntry)
        if (indexOfEntry == -1 || indexOfEntry == 0) {
            return
        }
        val firstEntry = tagGroup.entries.first()
        val newDateTime = firstEntry.entryTime.minus(1.milliseconds)
        setDate(journalEntry, newDateTime)
    }

    fun moveToPreviousDay(tagGroup: TagGroup) {
        viewModelScope.launch {
            tagGroup.entries.forEach { journalEntry ->
                val currentEntryTime = journalEntry.entryTime
                val previousDayTime = currentEntryTime.minus(1.days)
                repository.update(journalEntry.copy(entryTime = previousDayTime))
            }
        }
    }

    fun moveToNextDay(tagGroup: TagGroup) {
        viewModelScope.launch {
            tagGroup.entries.forEach { journalEntry ->
                val currentEntryTime = journalEntry.entryTime
                val nextDayTime = currentEntryTime.plus(1.days)
                repository.update(journalEntry.copy(entryTime = nextDayTime))
            }
        }
    }

    fun upload() {
        val dayGroup = state.value.dayGroup ?: return
        viewModelScope.launch {
            repository.upload(dayGroup.tagGroups.flatMap { it.entries })
        }
    }

    fun upload(tagGroup: TagGroup) {
        viewModelScope.launch {
            repository.upload(tagGroup.entries)
        }
    }

    fun upload(entry: JournalEntry) {
        viewModelScope.launch {
            repository.upload(listOf(entry))
        }
    }

    fun sync() {
        Logger.i(TAG) { "Attempting to sync" }
        viewModelScope.launch {
            repository.uploadAll()
        }
    }

    fun resolveConflict(
        entry: JournalEntry,
        selectedConflict: EntryConflict?,
    ) {
        viewModelScope.launch {
            repository.resolveConflict(entry, selectedConflict)
        }
    }

    fun onCopy(entry: JournalEntry) {
        contentForCopy.update { entry.text }
    }

    fun onCopy(tagGroup: TagGroup) {
        contentForCopy.update {
            tagGroup
                .entries
                .joinToString(separator = "\n") { it.text }
        }
    }

    fun onReconcile() {
        viewModelScope.launch(Dispatchers.Default) {
            val dayGroup = state.value.dayGroup ?: return@launch
            if (dayGroup.untaggedCount > 0) {
                Logger.i(TAG) { "Cannot export day group with untagged entries" }
                return@launch
            }
            val dayGroupEntryIds = dayGroup.tagGroups.flatMap { it.entries }.map { it.id }
            if (dayGroupEntryIds.isEmpty()) {
                Logger.i(TAG) { "Cannot export empty day group" }
                return@launch
            }
            if (state.value.entryConflicts.any { dayGroupEntryIds.contains(it.id) }) {
                Logger.i(TAG) { "Cannot export day group with conflicting entries" }
                return@launch
            }
            reconcile(dayGroup)
        }
    }

    fun onContentCopied() {
        contentForCopy.update { "" }
    }

    fun resetReceiveHelper() {
        ServiceLocator.resetReceiveHelper(resetWebsocket = true)
    }

    fun notify(
        entry: JournalEntry,
        inTime: Duration,
    ) {
        ServiceLocator.showNotification(entry, inTime)
    }

    private fun setDate(
        journalEntry: JournalEntry,
        entryTime: LocalDateTime,
    ) {
        viewModelScope.launch {
            repository.update(journalEntry.copy(entryTime = entryTime))
        }
    }

    private fun reconcile(dayGroup: DayGroup) {
        viewModelScope.launch {
            export(listOf(dayGroup))
            dayGroup
                .tagGroups
                .map { it.entries }
                .flatten()
                .map { it.copy(reconciled = true) }
                .let { repository.update(it) }
        }
    }

    private suspend fun export(dayGroups: List<DayGroup>) {
        val exportRepository = exportRepository ?: return
        val exportDirectory = prefManager.getExportDirectory().first()
        if (exportDirectory.isBlank()) {
            return
        }
        dayGroups.forEach { dayGroup ->
            exportRepository.export(
                baseDir = exportDirectory,
                forDate = dayGroup.date,
                content = dayGroup.content(),
            )
        }
    }

    private suspend fun DayGroup.content() =
        contentForExport(
            dayMonthDateWithYear = dayMonthDateWithYearSuspend(date),
            copyEmptyTags = prefManager.copyWithEmptyTags().first(),
        )

    companion object {
        fun factory(selectedDate: LocalDate) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: KClass<T>,
                    extras: CreationExtras,
                ): T {
                    return JournalEntryViewModel(
                        selectedDate = selectedDate,
                        repository = ServiceLocator.repository,
                        exportRepository = ServiceLocator.exportRepository,
                        tagsDao = ServiceLocator.tagsDao,
                        prefManager = ServiceLocator.prefManager,
                        allowNotify = ServiceLocator.allowJournalEntryNotify(),
                        webSocketHelper = ServiceLocator.webSocketHelper,
                    ) as T
                }
            }

        private const val TAG = "JournalEntryViewModel"
    }
}

data class ViewState(
    val dayGroup: DayGroup?,
    val tags: List<Tag> = listOf(),
    // Total across days
    val notUploadedCount: Int = 0,
    val entryConflicts: List<EntryConflict> = listOf(),
    val showConflictDiffInline: Boolean = false,
    val contentForCopy: String = "",
    val showEmptyTags: Boolean = false,
    val allowNotify: Boolean = false,
    val isConnected: Boolean = false,
) {
    private val entryIdsForDayGroup: List<String> =
        dayGroup
            ?.tagGroups
            ?.flatMap { tagGroup ->
                tagGroup.entries.map { it.id }
            } ?: emptyList()
    val dayGroupConflictCount: Int =
        entryConflicts
            .distinctBy { it.id }
            .count { it.id in entryIdsForDayGroup }
}
