package com.ramitsuri.notificationjournal.core.ui.journalentryday

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.model.DayGroup
import com.ramitsuri.notificationjournal.core.model.toDayGroups
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

class ViewJournalEntryDayViewModel(
    savedStateHandle: SavedStateHandle,
    repository: JournalRepository,
    tagsDao: TagsDao,
) : ViewModel() {
    private val selectedDate = MutableStateFlow<LocalDate?>(null)

    init {
        savedStateHandle.get<String>(DATE_ARG)?.let {
            selectedDate.value = LocalDate.parse(it)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state =
        selectedDate
            .filterNotNull()
            .flatMapLatest { date ->
                repository
                    .getForDateFlow(date)
                    .map { entries ->
                        ViewState(
                            dayGroup =
                                entries.toDayGroups(
                                    tagsForSort = tagsDao.getAll().map { it.value },
                                ).firstOrNull(),
                            selectedDate = date,
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

    companion object {
        const val DATE_ARG = "date"
    }
}

data class ViewState(
    val dayGroup: DayGroup? = null,
    val selectedDate: LocalDate? = null,
)
