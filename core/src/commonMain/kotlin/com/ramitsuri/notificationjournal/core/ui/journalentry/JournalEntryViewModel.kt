package com.ramitsuri.notificationjournal.core.ui.journalentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.DateWithCount
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.EntryConflict
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.TagGroup
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.stats.EntryStats
import com.ramitsuri.notificationjournal.core.model.toDayGroups
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import com.ramitsuri.notificationjournal.core.utils.combine
import com.ramitsuri.notificationjournal.core.utils.dayMonthDateWithYearSuspend
import com.ramitsuri.notificationjournal.core.utils.minus
import com.ramitsuri.notificationjournal.core.utils.nowLocal
import com.ramitsuri.notificationjournal.core.utils.plus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlin.math.absoluteValue
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

class JournalEntryViewModel(
    receivedText: String?,
    private val repository: JournalRepository,
    private val tagsDao: TagsDao,
    private val prefManager: PrefManager,
    private val clock: Clock = Clock.System,
    private val allowNotify: Boolean,
) : ViewModel() {
    private val _receivedText: MutableStateFlow<String?> = MutableStateFlow(receivedText)
    val receivedText: StateFlow<String?> = _receivedText

    private val selectedDate = MutableStateFlow<LocalDate?>(null)

    private val contentForCopy: MutableStateFlow<String> = MutableStateFlow("")

    private val snackBarType: MutableStateFlow<SnackBarType> = MutableStateFlow(SnackBarType.None)

    private val statsRequested: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var reconcileDayGroupJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ViewState> =
        combine(
            selectedDate,
            repository.getNotReconciledEntryDatesFlow(),
        ) { selectedDate, countAndDates ->
            val today = clock.nowLocal().date
            if (selectedDate == null) {
                val closestDateToToday =
                    countAndDates.minByOrNull { countAndDate ->
                        today.minus(countAndDate.date).seconds.absoluteValue
                    }?.date
                this.selectedDate.update {
                    closestDateToToday ?: countAndDates.lastOrNull()?.date
                }
            }
            countAndDates to (selectedDate ?: today)
        }.flatMapLatest { (countAndDates, selectedDate) ->
            combine(
                contentForCopy,
                snackBarType,
                repository.getForDateFlow(selectedDate),
                repository.getForUploadCountFlow(),
                repository.getConflicts(),
                prefManager.showEmptyTags(),
                prefManager.showConflictDiffInline(),
                prefManager.showLogsButton(),
                statsRequested,
            ) { contentForCopy, snackBarType, entries, forUploadCount, entryConflicts,
                showEmptyTags, showConflictDiffInline, showLogsButton, statsRequested,
                ->
                val tags = tagsDao.getAll()
                val entryIds = entries.map { it.id }
                ViewState(
                    dateWithCountList = countAndDates,
                    dayGroup = entries.toDayGroups().firstOrNull() ?: ViewState.defaultDayGroup,
                    tags = tags,
                    notUploadedCount = forUploadCount,
                    entryConflicts = entryConflicts.filter { entryIds.contains(it.entryId) },
                    showConflictDiffInline = showConflictDiffInline,
                    contentForCopy = contentForCopy,
                    showEmptyTags = showEmptyTags,
                    snackBarType = snackBarType,
                    showLogsButton = showLogsButton,
                    stats = if (statsRequested) repository.getStats() else null,
                    allowNotify = allowNotify,
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

    fun forceUpload(tagGroup: TagGroup) {
        viewModelScope.launch {
            repository.upload(tagGroup.entries)
        }
    }

    fun forceUpload(entry: JournalEntry) {
        viewModelScope.launch {
            repository.upload(listOf(entry))
        }
    }

    fun sync() {
        Logger.i("JournalEntryViewModel") { "Attempting to sync" }
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

    fun goToNextDay() {
        selectedDate.update { existing ->
            val dateWithCountList = state.value.dateWithCountList
            if (dateWithCountList.isEmpty()) {
                return
            }
            if (existing == null) {
                return
            }
            val existingIndex = state.value.dateWithCountList.indexOfFirst { it.date == existing }
            val newIndex = existingIndex + 1
            if (newIndex > dateWithCountList.lastIndex) {
                dateWithCountList.first().date
            } else {
                dateWithCountList[newIndex].date
            }
        }
    }

    fun goToPreviousDay() {
        selectedDate.update { existing ->
            val dateWithCountList = state.value.dateWithCountList
            if (dateWithCountList.isEmpty()) {
                return
            }
            if (existing == null) {
                return
            }
            val existingIndex = state.value.dateWithCountList.indexOfFirst { it.date == existing }
            val newIndex = existingIndex - 1
            if (newIndex < 0) {
                dateWithCountList.last().date
            } else {
                dateWithCountList[newIndex].date
            }
        }
    }

    fun showDayGroupClicked(date: LocalDate) {
        selectedDate.update { date }
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

    fun onCopy() {
        val conflicts = state.value.entryConflicts
        val dayGroup = state.value.dayGroup
        if (dayGroup == ViewState.defaultDayGroup) {
            return
        }
        if (dayGroup.untaggedCount > 0) {
            return
        }
        val dayGroupEntryIds = dayGroup.tagGroups.flatMap { it.entries }.map { it.id }
        if (dayGroupEntryIds.isEmpty()) {
            return
        }
        if (conflicts.any { dayGroupEntryIds.contains(it.id) }) {
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            val copyEmptyTags = prefManager.copyWithEmptyTags().first()
            val content =
                buildString {
                    append("# ")
                    append(dayMonthDateWithYearSuspend(dayGroup.date))
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
            contentForCopy.update { content }
            reconcileWithDelay(dayGroup)
        }
    }

    fun onContentCopied() {
        contentForCopy.update { "" }
    }

    fun resetReceiveHelper() {
        ServiceLocator.resetReceiveHelper()
    }

    fun onStatsRequestToggled() {
        statsRequested.update { !it }
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

    private fun reconcileWithDelay(dayGroup: DayGroup) {
        snackBarType.update {
            SnackBarType.Reconcile
        }
        reconcileDayGroupJob =
            viewModelScope.launch {
                delay(1_000)
                snackBarType.update {
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
        fun factory(receivedText: String?) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: KClass<T>,
                    extras: CreationExtras,
                ): T {
                    return JournalEntryViewModel(
                        receivedText,
                        ServiceLocator.repository,
                        ServiceLocator.tagsDao,
                        prefManager = ServiceLocator.prefManager,
                        allowNotify = ServiceLocator.allowJournalEntryNotify(),
                    ) as T
                }
            }
    }
}

data class ViewState(
    val dateWithCountList: List<DateWithCount> = listOf(),
    val dayGroup: DayGroup = defaultDayGroup,
    val tags: List<Tag> = listOf(),
    val notUploadedCount: Int = 0,
    val entryConflicts: List<EntryConflict> = listOf(),
    val showConflictDiffInline: Boolean = false,
    val contentForCopy: String = "",
    val showEmptyTags: Boolean = false,
    val showLogsButton: Boolean = false,
    val snackBarType: SnackBarType = SnackBarType.None,
    val stats: EntryStats? = null,
    val allowNotify: Boolean = false,
) {
    companion object {
        val defaultDayGroup = DayGroup(LocalDate(1970, 1, 1), listOf())
    }
}

sealed interface SnackBarType {
    data object None : SnackBarType

    data object Reconcile : SnackBarType
}
