package com.ramitsuri.notificationjournal.core.ui.nav

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.ramitsuri.notificationjournal.core.ui.addjournal.AddJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.editjournal.EditJournalEntryViewModel

enum class Destination(private val route: String) {
    JOURNAL_ENTRY("journal_entry"),
    TAGS("tags"),
    TEMPLATES("templates"),
    SETTINGS("settings"),
    ADD_ENTRY("add_entry"),
    EDIT_ENTRY("edit_entry"),
    LOGS("logs"),
    SEARCH("search"),
    IMPORT("import"),
    ;

    fun routeWithArgValues(args: Map<String, String?> = mapOf()): String {
        return when {
            this == EDIT_ENTRY -> {
                route.plus("/${args[EditJournalEntryViewModel.ENTRY_ID_ARG]}")
            }

            this == ADD_ENTRY -> {
                route
                    .plus("?${AddJournalEntryViewModel.RECEIVED_TEXT_ARG}")
                    .plus("=${args[AddJournalEntryViewModel.RECEIVED_TEXT_ARG]}")
                    .plus("&${AddJournalEntryViewModel.DUPLICATE_FROM_ENTRY_ID_ARG}")
                    .plus("=${args[AddJournalEntryViewModel.DUPLICATE_FROM_ENTRY_ID_ARG]}")
                    .plus("&${AddJournalEntryViewModel.DATE_ARG}")
                    .plus("=${args[AddJournalEntryViewModel.DATE_ARG]}")
                    .plus("&${AddJournalEntryViewModel.TIME_ARG}")
                    .plus("=${args[AddJournalEntryViewModel.TIME_ARG]}")
                    .plus("&${AddJournalEntryViewModel.TAG_ARG}")
                    .plus("=${args[AddJournalEntryViewModel.TAG_ARG]}")
            }

            else -> {
                route
            }
        }
    }

    fun route(): String {
        return when {
            this == EDIT_ENTRY -> {
                route.plus("/{${EditJournalEntryViewModel.ENTRY_ID_ARG}}")
            }

            this == ADD_ENTRY -> {
                route
                    .plus("?${AddJournalEntryViewModel.RECEIVED_TEXT_ARG}")
                    .plus("={${AddJournalEntryViewModel.RECEIVED_TEXT_ARG}}")
                    .plus("&${AddJournalEntryViewModel.DUPLICATE_FROM_ENTRY_ID_ARG}")
                    .plus("={${AddJournalEntryViewModel.DUPLICATE_FROM_ENTRY_ID_ARG}}")
                    .plus("&${AddJournalEntryViewModel.DATE_ARG}")
                    .plus("={${AddJournalEntryViewModel.DATE_ARG}}")
                    .plus("&${AddJournalEntryViewModel.TIME_ARG}")
                    .plus("={${AddJournalEntryViewModel.TIME_ARG}}")
                    .plus("&${AddJournalEntryViewModel.TAG_ARG}")
                    .plus("={${AddJournalEntryViewModel.TAG_ARG}}")
            }

            else -> {
                route
            }
        }
    }

    fun navArgs(): List<NamedNavArgument> {
        return when {
            this == EDIT_ENTRY -> {
                listOf(
                    navArgument(EditJournalEntryViewModel.ENTRY_ID_ARG) {
                        type = NavType.StringType
                        nullable = false
                    },
                )
            }

            this == ADD_ENTRY -> {
                listOf(
                    navArgument(AddJournalEntryViewModel.RECEIVED_TEXT_ARG) {
                        type = NavType.StringType
                        nullable = true
                    },
                    navArgument(AddJournalEntryViewModel.DUPLICATE_FROM_ENTRY_ID_ARG) {
                        type = NavType.StringType
                        nullable = true
                    },
                    navArgument(AddJournalEntryViewModel.DATE_ARG) {
                        type = NavType.StringType
                        nullable = true
                    },
                    navArgument(AddJournalEntryViewModel.TIME_ARG) {
                        type = NavType.StringType
                        nullable = true
                    },
                    navArgument(AddJournalEntryViewModel.TAG_ARG) {
                        type = NavType.StringType
                        nullable = true
                    },
                )
            }

            else -> {
                listOf()
            }
        }
    }
}
