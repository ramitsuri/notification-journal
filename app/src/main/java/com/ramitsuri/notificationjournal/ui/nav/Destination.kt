package com.ramitsuri.notificationjournal.ui.nav

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.ramitsuri.notificationjournal.ui.addjournal.AddJournalEntryViewModel
import com.ramitsuri.notificationjournal.ui.editjournal.EditJournalEntryViewModel

enum class Destination(private val route: String) {
    JOURNAL_ENTRY("journal_entry"),
    TAGS("tags"),
    SETTINGS("settings"),
    ADD_ENTRY("add_entry"),
    EDIT_ENTRY("edit_entry"),
    ;

    fun routeWithArgValues(args: Map<String, String> = mapOf()): String {
        return if (this == EDIT_ENTRY) {
            route.plus("/${args[EditJournalEntryViewModel.ENTRY_ID_ARG]}")
        } else if (this == ADD_ENTRY) {
            route.plus("/${args[AddJournalEntryViewModel.RECEIVED_TEXT_ARG]}")
        } else {
            route
        }
    }

    fun route(): String {
        return if (this == EDIT_ENTRY) {
            route.plus("/{${EditJournalEntryViewModel.ENTRY_ID_ARG}}")
        } else if (this == ADD_ENTRY) {
            route.plus("/{${AddJournalEntryViewModel.RECEIVED_TEXT_ARG}}")
        } else {
            route
        }
    }

    fun navArgs(): List<NamedNavArgument> {
        return if (this == EDIT_ENTRY) {
            listOf(
                navArgument(EditJournalEntryViewModel.ENTRY_ID_ARG) {
                    type = NavType.IntType
                    nullable = false
                },
            )
        } else if (this == ADD_ENTRY) {
            listOf(
                navArgument(AddJournalEntryViewModel.RECEIVED_TEXT_ARG) {
                    type = NavType.StringType
                    nullable = true
                },
            )
        } else {
            listOf()
        }
    }
}
