package com.ramitsuri.notificationjournal.core.ui.journalentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.data.VoiceNoteDao
import com.ramitsuri.notificationjournal.core.di.PlatformConfig
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.EntryConflict
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.TagGroup
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.toDayGroups
import com.ramitsuri.notificationjournal.core.model.voice.VoiceNote
import com.ramitsuri.notificationjournal.core.model.voice.VoiceNoteStatus
import com.ramitsuri.notificationjournal.core.network.WebSocketHelper
import com.ramitsuri.notificationjournal.core.repository.ExportRepository
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.sync.AppLifecycleCoordinator
import com.ramitsuri.notificationjournal.core.utils.JournalEntryNotificationHelper
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import com.ramitsuri.notificationjournal.core.utils.combine
import com.ramitsuri.notificationjournal.core.utils.dayMonthDateWithYearSuspend
import com.ramitsuri.notificationjournal.core.utils.minus
import com.ramitsuri.notificationjournal.core.utils.plus
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class JournalEntryViewModel
    @AssistedInject
    constructor(
        @Assisted initialDate: LocalDate,
        private val repository: JournalRepository,
        private val exportRepository: ExportRepository?,
        private val tagsDao: TagsDao,
        private val prefManager: PrefManager,
        webSocketHelper: WebSocketHelper,
        voiceNoteDao: VoiceNoteDao,
        private val lifecycleCoordinator: AppLifecycleCoordinator,
        platformConfig: PlatformConfig,
        private val notificationHelper: JournalEntryNotificationHelper?,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(initialDate: LocalDate): JournalEntryViewModel
        }

        private val allowNotify: Boolean = platformConfig.allowJournalEntryNotify

        private val contentForCopy: MutableStateFlow<String> = MutableStateFlow("")
        private val selectedDate: MutableStateFlow<LocalDate> = MutableStateFlow(initialDate)

        val state: StateFlow<ViewState> =
            combine(
                contentForCopy,
                selectedDate.flatMapLatest { date ->
                    repository.getForDateFlow(date).map { entries -> date to entries }
                },
                repository.getForUploadCountFlow(),
                repository.getConflicts(),
                prefManager.showEmptyTags(),
                prefManager.showConflictDiffInline(),
                webSocketHelper.isConnected,
                voiceNoteDao.getPendingOrFailedFlow(),
            ) {
                contentForCopy,
                (date, entries),
                forUploadCount,
                entryConflicts,
                showEmptyTags,
                showConflictDiffInline,
                isConnected,
                voiceNotes,
                ->
                val tags = tagsDao.getAll()
                val entryIds = entries.map { it.id }
                ViewState(
                    dayGroup =
                        entries
                            .toDayGroups(tags.map { it.value })
                            .firstOrNull()
                            ?: DayGroup(date = date, tagGroups = listOf()),
                    tags = tags,
                    notUploadedCount = forUploadCount,
                    entryConflicts = entryConflicts.filter { entryIds.contains(it.entryId) },
                    showConflictDiffInline = showConflictDiffInline,
                    contentForCopy = contentForCopy,
                    showEmptyTags = showEmptyTags,
                    allowNotify = allowNotify,
                    isConnected = isConnected,
                    voiceNotes = voiceNotes,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ViewState(dayGroup = null),
            )

        fun onDateSelected(date: LocalDate) {
            selectedDate.value = date
        }

        /**
         * Day group for the currently selected date, null if it's still loading (for example, right after
         * the date was switched and the previous date's data is still in [state]).
         */
        private val currentDayGroup: DayGroup?
            get() = state.value.dayGroup?.takeIf { it.date == selectedDate.value }

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
            val dayGroup = currentDayGroup ?: return
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
                val dayGroup = currentDayGroup ?: return@launch
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
            lifecycleCoordinator.resetReceiveHelper(resetWebsocket = true)
        }

        fun notify(
            entry: JournalEntry,
            inTime: Duration,
        ) {
            notificationHelper?.show(entry, inTime)
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
    val voiceNotes: List<VoiceNote> = listOf(),
) {
    val pendingVoiceNotesCount: Int
        get() = voiceNotes.count { it.status == VoiceNoteStatus.PROCESSING }

    val failedVoiceNotes: List<VoiceNote>
        get() = voiceNotes.filter { it.status == VoiceNoteStatus.FAILED }

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
