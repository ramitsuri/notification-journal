package com.ramitsuri.notificationjournal.core.ui.journalentry

import kotlinx.datetime.LocalDate

sealed interface EntryScreenAction {
    data class AddWithDate(val date: LocalDate? = null) : EntryScreenAction

    data object NavToSettings : EntryScreenAction

    data object Sync : EntryScreenAction

    data object Copy : EntryScreenAction

    data object ResetReceiveHelper : EntryScreenAction

    data object NavToSearch : EntryScreenAction

    data object NavToViewJournalEntryDay : EntryScreenAction

    data object NavBack : EntryScreenAction
}
