package com.ramitsuri.notificationjournal.core.ui.journalentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.EntryConflict
import com.ramitsuri.notificationjournal.core.model.SortOrder
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.TagGroup
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.toDayGroups
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.KeyValueStore
import com.ramitsuri.notificationjournal.core.utils.getLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val keyValueStore: KeyValueStore,
    private val repository: JournalRepository,
    private val tagsDao: TagsDao,
    private val zoneId: TimeZone = TimeZone.currentSystemDefault(),
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _receivedText: MutableStateFlow<String?> = MutableStateFlow(receivedText)
    val receivedText: StateFlow<String?> = _receivedText

    private val _selectedPage: MutableStateFlow<Int?> = MutableStateFlow(null)

    val state: StateFlow<ViewState> = combine(
        _selectedPage,
        repository.getFlow(
            showReconciled = keyValueStore.getBoolean(
                Constants.PREF_KEY_SHOW_RECONCILED,
                false
            )
        ),
        repository.getForUploadCountFlow(),
        repository.getConflicts(),
    ) { selectedPage, entries, forUploadCount, entryConflicts ->
        val tags = tagsDao.getAll()
        val dayGroups = try {
            entries.toDayGroups(
                zoneId = zoneId,
                tagsForSort = tags,
            )
        } catch (e: Exception) {
            listOf()
        }
        val sorted = when (getSortOrder()) {
            SortOrder.ASC -> dayGroups.sortedBy { it.date }
            SortOrder.DESC -> dayGroups.sortedByDescending { it.date }
        }
        // Show either today or the biggest date as initially selected
        if (selectedPage == null) {
            _selectedPage.update {
                val initialIndex = sorted
                    .indexOfFirst {
                        it.date == getLocalDate(clock.now(), zoneId)
                    }
                    .takeIf { it >= 0 }
                    ?: sorted.lastIndex

                // To start somewhere around the middle of all the pages so we don't run
                // out of scrolling capability on either end
                dayGroupIndexToPage(
                    index = initialIndex,
                    totalPages = sorted.size.times(PAGES_MULTIPLIER),
                )
            }
            ViewState()
        } else {
            ViewState(
                dayGroups = sorted,
                tags = tags,
                notUploadedCount = forUploadCount,
                entryConflicts = entryConflicts,
                showConflictDiffInline = keyValueStore.getBoolean(
                    Constants.PREF_SHOW_CONFLICT_DIFF_INLINE,
                    false
                ),
                selectedPage = selectedPage,
            )
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
            tagGroup.entries.forEach { journalEntry ->
                repository.delete(journalEntry)
            }
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

    fun reconcile(tagGroup: TagGroup) {
        viewModelScope.launch {
            tagGroup.entries.forEach { journalEntry ->
                repository.update(journalEntry.copy(reconciled = true))
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
        _selectedPage.update { existing ->
            if (existing == null) {
                return
            }
            existing + 1
        }
    }

    fun goToPreviousDay() {
        _selectedPage.update { existing ->
            if (existing == null) {
                return
            }
            existing - 1
        }
    }

    fun showDayGroupClicked(dayGroup: DayGroup) {
        val existing = state.value
        val index = existing.dayGroups.indexOf(dayGroup)
        val page = dayGroupIndexToPage(
            index = index,
            totalPages = existing.horizontalPagerNumOfPages,
        )
        _selectedPage.update {
            page
        }
    }

    private fun setDate(journalEntry: JournalEntry, entryTime: Instant) {
        viewModelScope.launch {
            repository.update(journalEntry.copy(entryTime = entryTime))
        }
    }

    private fun getSortOrder(): SortOrder {
        val preferredSortOrderKey = keyValueStore.getInt(Constants.PREF_KEY_SORT_ORDER, 0)
        return SortOrder.fromKey(preferredSortOrderKey)
    }

    private fun dayGroupIndexToPage(index: Int, totalPages: Int): Int {
        return totalPages
            .div(2)
            .plus(index)
    }

    companion object {
        fun factory(receivedText: String?) = object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                return JournalEntryViewModel(
                    receivedText,
                    ServiceLocator.keyValueStore,
                    ServiceLocator.repository,
                    ServiceLocator.tagsDao,
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
    val selectedPage: Int = 0,
) {
    // To enable infinite scrolling, will have PAGES_MULTIPLIER times total
    // number of pages, so that can keep scrolling on either side
    val horizontalPagerNumOfPages: Int = dayGroups.size.times(PAGES_MULTIPLIER)
    val selectedDayGroup: DayGroup
        get() {
            if (dayGroups.isEmpty()) {
                return DayGroup(
                    LocalDate(1970, 1, 1),
                    listOf(),
                )
            }
            val index = selectedPage % dayGroups.size
            return dayGroups[index]
        }

    val dayGroupConflictCountMap
        get() = dayGroups
            .associateWith { it.getConflictsCount(entryConflicts) }
}

private const val PAGES_MULTIPLIER = 10
