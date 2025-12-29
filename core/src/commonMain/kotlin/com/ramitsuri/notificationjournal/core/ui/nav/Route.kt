package com.ramitsuri.notificationjournal.core.ui.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object JournalEntryDays : Route

    @Serializable
    data class JournalEntry(
        @SerialName(Args.JOURNAL_ENTRY_DATE)
        val selectedDate: LocalDate,
        @SerialName(Args.JOURNAL_ENTRY_ID)
        val highlightEntryId: String? = null,
    ) : Route

    @Serializable
    data object Tags : Route

    @Serializable
    data object Templates : Route

    @Serializable
    data object Settings : Route

    @ConsistentCopyVisibility
    @Serializable
    data class AddEntry private constructor(
        @SerialName("received_text")
        val receivedText: String? = null,
        @SerialName("duplicate_from_entry_id")
        val duplicateFromEntryId: String? = null,
        @SerialName("date_arg")
        val date: LocalDate? = null,
        @SerialName("time_arg")
        val time: LocalTime? = null,
        @SerialName("tag_arg")
        val tag: String? = null,
    ) : Route {
        companion object {
            fun fromReceivedText(
                text: String?,
                tag: String?,
            ) = AddEntry(
                receivedText = text,
                tag = tag,
            )

            fun fromDate(date: LocalDate?) =
                AddEntry(
                    date = date,
                )

            fun fromDateTimeTag(
                date: LocalDate,
                time: LocalTime?,
                tag: String?,
            ) = AddEntry(
                date = date,
                time = time,
                tag = tag,
            )

            fun fromDuplicateEntryId(entryId: String) =
                AddEntry(
                    duplicateFromEntryId = entryId,
                )
        }
    }

    @Serializable
    data class EditEntry(
        @SerialName("entry_id")
        val entryId: String,
    ) : Route

    @Serializable
    data object Logs : Route

    @Serializable
    data object Search : Route

    @Serializable
    data object Import : Route

    @Serializable
    data class ViewJournalEntryDay(
        @SerialName("date")
        val date: LocalDate? = null,
        @SerialName("entry_id")
        val entryId: String? = null,
    ) : Route
}
