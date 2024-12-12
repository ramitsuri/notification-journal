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
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import com.ramitsuri.notificationjournal.core.utils.combine
import com.ramitsuri.notificationjournal.core.utils.dayMonthDateWithYear
import com.ramitsuri.notificationjournal.core.utils.getLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

class JournalEntryViewModel(
    receivedText: String?,
    private val repository: JournalRepository,
    private val tagsDao: TagsDao,
    private val prefManager: PrefManager,
    private val zoneId: TimeZone = TimeZone.currentSystemDefault(),
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _receivedText: MutableStateFlow<String?> = MutableStateFlow(receivedText)
    val receivedText: StateFlow<String?> = _receivedText

    private val _selectedIndex: MutableStateFlow<Int?> = MutableStateFlow(null)

    private val _contentForCopy: MutableStateFlow<String> = MutableStateFlow("")

    private val _snackBarType: MutableStateFlow<SnackBarType> = MutableStateFlow(SnackBarType.None)

    private var reconcileDayGroupJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ViewState> =
        prefManager.showReconciled().flatMapLatest { showReconciled ->
            combine(
                _selectedIndex,
                _contentForCopy,
                _snackBarType,
                repository.getFlow(showReconciled = showReconciled),
                repository.getForUploadCountFlow(),
                repository.getConflicts(),
                prefManager.showEmptyTags(),
                prefManager.showConflictDiffInline(),
                prefManager.showLogsButton(),
            ) { selectedIndex, contentForCopy, snackBarType, entries, forUploadCount, entryConflicts,
                showEmptyTags, showConflictDiffInline, showLogsButton ->
                val tags = tagsDao.getAll()
                val dayGroups = try {
                    entries.toDayGroups(
                        zoneId = zoneId,
                        tagsForSort = tags,
                    )
                } catch (e: Exception) {
                    listOf()
                }
                val sorted = dayGroups.sortedBy { it.date }
                // Show either today or the biggest date as initially selected
                if (selectedIndex == null) {
                    if (sorted.isNotEmpty()) {
                        _selectedIndex.update {
                            sorted
                                .indexOfFirst {
                                    it.date == getLocalDate(clock.now(), zoneId)
                                }
                                .takeIf { it >= 0 }
                                ?: sorted.lastIndex
                        }
                    }
                    ViewState()
                } else {
                    ViewState(
                        dayGroups = sorted,
                        tags = tags,
                        notUploadedCount = forUploadCount,
                        entryConflicts = entryConflicts,
                        showConflictDiffInline = showConflictDiffInline,
                        selectedIndex = selectedIndex,
                        contentForCopy = contentForCopy,
                        showEmptyTags = showEmptyTags,
                        snackBarType = snackBarType,
                        showLogsButton = showLogsButton,
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ViewState(),
        )

    fun setReceivedText(text: String?) {
        if (!text.isNullOrEmpty()) {
            _receivedText.update {
                text
            }
        }
    }

    fun onReceivedTextConsumed() {
        _receivedText.update {
            null
        }
    }

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

    fun editTag(journalEntry: JournalEntry, tag: String) {
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
    fun moveDown(journalEntry: JournalEntry, tagGroup: TagGroup) {
        val indexOfEntry = tagGroup.entries.indexOf(journalEntry)
        if (indexOfEntry == -1 || indexOfEntry == tagGroup.entries.lastIndex) {
            return
        }
        val nextEntry = tagGroup.entries[indexOfEntry + 1]
        val newDateTime = nextEntry.entryTime.plus(1.milliseconds)
        setDate(journalEntry, newDateTime)
    }

    // Move it to bottom of the list of entries that are ordered by ascending of display time
    fun moveToBottom(journalEntry: JournalEntry, tagGroup: TagGroup) {
        val indexOfEntry = tagGroup.entries.indexOf(journalEntry)
        if (indexOfEntry == -1 || indexOfEntry == tagGroup.entries.lastIndex) {
            return
        }
        val lastEntry = tagGroup.entries.last()
        val newDateTime = lastEntry.entryTime.plus(1.milliseconds)
        setDate(journalEntry, newDateTime)
    }

    // Move it up in the list of entries that are ordered by ascending of display time
    fun moveUp(journalEntry: JournalEntry, tagGroup: TagGroup) {
        val indexOfEntry = tagGroup.entries.indexOf(journalEntry)
        if (indexOfEntry == -1 || indexOfEntry == 0) {
            return
        }
        val previousEntry = tagGroup.entries[indexOfEntry - 1]
        val newDateTime = previousEntry.entryTime.minus(1.milliseconds)
        setDate(journalEntry, newDateTime)
    }

    // Move it to top of the list of entries that are ordered by ascending of display time
    fun moveToTop(journalEntry: JournalEntry, tagGroup: TagGroup) {
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

    fun forceUpload(tagGroup: TagGroup) {
        repository.upload(tagGroup.entries)
    }

    fun forceUpload(entry: JournalEntry) {
        repository.upload(listOf(entry))
    }

    fun sync() {
        Logger.i("JournalEntryViewModel") { "Attempting to sync" }
        viewModelScope.launch {
            repository.sync()
        }
    }

    fun resolveConflict(entry: JournalEntry, selectedConflict: EntryConflict?) {
        viewModelScope.launch {
            repository.resolveConflict(entry, selectedConflict)
        }
    }

    fun goToNextDay() {
        _selectedIndex.update { existing ->
            if (existing == null) {
                return
            }
            val new = existing + 1
            if (new > state.value.dayGroups.lastIndex) {
                0
            } else {
                new
            }
        }
    }

    fun goToPreviousDay() {
        _selectedIndex.update { existing ->
            if (existing == null) {
                return
            }
            val new = existing - 1
            if (new < 0) {
                state.value.dayGroups.lastIndex
            } else {
                new
            }
        }
    }

    fun showDayGroupClicked(dayGroup: DayGroup) {
        val existing = state.value
        val index = existing.dayGroups.indexOf(dayGroup)
        _selectedIndex.update { index }
    }

    fun onCopy(entry: JournalEntry) {
        _contentForCopy.update { entry.text }
    }

    fun onCopy(tagGroup: TagGroup) {
        _contentForCopy.update {
            tagGroup
                .entries
                .joinToString(separator = "\n") { it.text }
        }
    }

    fun onCopy() {
        val dayGroup = state.value.selectedDayGroup
        if (dayGroup.untaggedCount > 0) {
            return
        }
        if ((state.value.dayGroupConflictCountMap[dayGroup] ?: 0) > 0) {
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            val copyEmptyTags = prefManager.copyWithEmptyTags().first()
            val content = buildString {
                append("# ")
                append(dayMonthDateWithYear(dayGroup.date))
                append("\n")
                dayGroup.tagGroups.forEach { tagGroup ->
                    if (tagGroup.entries.isEmpty() && !copyEmptyTags) {
                        return@forEach
                    }
                    append("## ")
                    append(tagGroup.tag)
                    append("\n")
                    tagGroup.entries.forEach { entry ->
                        append("- ")
                        append(entry.text)
                        append("\n")
                    }
                }
            }
            _contentForCopy.update { content }
            reconcileWithDelay(dayGroup)
        }
    }

    fun onContentCopied() {
        _contentForCopy.update { "" }
    }

    fun resetReceiveHelper() {
        ServiceLocator.resetReceiveHelper()
    }

    private fun setDate(journalEntry: JournalEntry, entryTime: Instant) {
        viewModelScope.launch {
            repository.update(journalEntry.copy(entryTime = entryTime))
        }
    }

    private fun reconcileWithDelay(dayGroup: DayGroup) {
        _snackBarType.update {
            SnackBarType.Reconcile
        }
        reconcileDayGroupJob = viewModelScope.launch {
            delay(1_000)
            _snackBarType.update {
                SnackBarType.None
            }
            delay(5_000)
            dayGroup
                .tagGroups
                .map { it.entries }
                .flatten()
                .map { it.copy(reconciled = true) }
                .let { repository.update(it) }
        }
    }

    fun cancelReconcile() {
        reconcileDayGroupJob?.cancel()
        reconcileDayGroupJob = null
    }

    companion object {
        fun factory(receivedText: String?) = object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: KClass<T>,
                extras: CreationExtras
            ): T {
                return JournalEntryViewModel(
                    receivedText,
                    ServiceLocator.repository,
                    ServiceLocator.tagsDao,
                    prefManager = ServiceLocator.prefManager,
                ) as T
            }
        }
    }
}

data class ViewState(
    val dayGroups: List<DayGroup> = listOf(),
    val tags: List<Tag> = listOf(),
    val notUploadedCount: Int = 0,
    val entryConflicts: List<EntryConflict> = listOf(),
    val showConflictDiffInline: Boolean = false,
    val selectedIndex: Int = 0,
    val contentForCopy: String = "",
    val showEmptyTags: Boolean = false,
    val showLogsButton: Boolean = false,
    val snackBarType: SnackBarType = SnackBarType.None,
) {
    val selectedDayGroup: DayGroup
        get() {
            return dayGroups.getOrNull(selectedIndex) ?: DayGroup(
                LocalDate(1970, 1, 1),
                listOf(),
            )
        }

    val dayGroupConflictCountMap
        get() = dayGroups
            .associateWith { it.getConflictsCount(entryConflicts) }
}

sealed interface SnackBarType {
    data object None : SnackBarType
    data object Reconcile : SnackBarType
}
