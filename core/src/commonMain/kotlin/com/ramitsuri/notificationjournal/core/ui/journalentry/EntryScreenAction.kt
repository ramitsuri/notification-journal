package com.ramitsuri.notificationjournal.core.ui.journalentry

import com.ramitsuri.notificationjournal.core.model.DayGroup
import kotlinx.datetime.LocalDate

sealed interface EntryScreenAction {
    data class AddWithDate(val date: LocalDate? = null) : EntryScreenAction

    data class ShowDayGroup(val dayGroup: DayGroup) : EntryScreenAction

    data object NavToSettings : EntryScreenAction

    data object Sync : EntryScreenAction

    data object Copy : EntryScreenAction

    data object ResetReceiveHelper : EntryScreenAction

    data object CancelReconcile : EntryScreenAction

    data object NavToLogs : EntryScreenAction

    data object NavToSearch : EntryScreenAction

    data object ShowStatsToggled : EntryScreenAction
}
