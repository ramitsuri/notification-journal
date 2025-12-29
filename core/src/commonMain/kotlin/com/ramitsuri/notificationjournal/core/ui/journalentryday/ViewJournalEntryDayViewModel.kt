package com.ramitsuri.notificationjournal.core.ui.journalentryday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.toDayGroups
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.ui.components.EntryDayHighlight
import com.ramitsuri.notificationjournal.core.ui.nav.Route
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

class ViewJournalEntryDayViewModel(
    arg: Route.ViewJournalEntryDay,
    repository: JournalRepository,
    tagsDao: TagsDao,
) : ViewModel() {
    private val selectedDate = MutableStateFlow(arg.date)
    private val contentForCopy: MutableStateFlow<String> = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val state =
        selectedDate
            .filterNotNull()
            .flatMapLatest { date ->
                combine(
                    repository.getForDateFlow(date),
                    contentForCopy,
                ) { entries, contentForCopy ->
                    val dayGroup =
                        entries.toDayGroups(
                            tagsForSort = tagsDao.getAll().map { it.value },
                        ).firstOrNull()
                    ViewState(
                        dayGroup = dayGroup,
                        selectedDate = date,
                        contentForCopy = contentForCopy,
                        highlight =
                            arg.entryId?.let { entryId ->
                                val index = getEntryIndex(dayGroup, entryId)
                                if (index != null) {
                                    EntryDayHighlight(index, entryId)
                                } else {
                                    null
                                }
                            },
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ViewState(),
            )

    fun onDateSelected(date: LocalDate) {
        selectedDate.value = date
    }

    fun onCopy(entry: JournalEntry) {
        contentForCopy.update { entry.text }
    }

    fun onContentCopied() {
        contentForCopy.update { "" }
    }

    private fun getEntryIndex(
        dayGroup: DayGroup?,
        entryId: String?,
    ): Int? {
        if (dayGroup == null) {
            return null
        }
        if (entryId == null) {
            return null
        }
        var index = -1
        dayGroup.tagGroups.filter { it.entries.isNotEmpty() }.forEach { tagGroup ->
            index++
            tagGroup.entries.forEach { tagGroupEntry ->
                index++
                if (tagGroupEntry.id == entryId) {
                    return index
                }
            }
        }
        return null
    }
}

data class ViewState(
    val dayGroup: DayGroup? = null,
    val selectedDate: LocalDate? = null,
    val contentForCopy: String = "",
    val highlight: EntryDayHighlight? = null,
)
