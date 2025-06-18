package com.ramitsuri.notificationjournal.core.ui.components

import com.ramitsuri.notificationjournal.core.model.EntryConflict
import com.ramitsuri.notificationjournal.core.model.TagGroup
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

sealed interface DayGroupAction {
    data class AddEntry(val date: LocalDate, val time: LocalTime?, val tag: String?) :
        DayGroupAction

    data class EditEntry(val entry: JournalEntry) : DayGroupAction

    data class DeleteEntry(val entry: JournalEntry) : DayGroupAction

    data class MoveEntryToNextDay(val entry: JournalEntry) : DayGroupAction

    data class MoveEntryToPreviousDay(val entry: JournalEntry) : DayGroupAction

    data class DuplicateEntry(val entry: JournalEntry) : DayGroupAction

    data class ForceUploadEntry(val entry: JournalEntry) : DayGroupAction

    data class CopyEntry(val entry: JournalEntry) : DayGroupAction

    data class MoveEntryUp(val entry: JournalEntry, val tagGroup: TagGroup) : DayGroupAction

    data class MoveEntryToTop(val entry: JournalEntry, val tagGroup: TagGroup) : DayGroupAction

    data class MoveEntryDown(val entry: JournalEntry, val tagGroup: TagGroup) : DayGroupAction

    data class MoveEntryToBottom(val entry: JournalEntry, val tagGroup: TagGroup) :
        DayGroupAction

    data class EditTag(val entry: JournalEntry, val tag: String) : DayGroupAction

    data class MoveTagGroupToNextDay(val tagGroup: TagGroup) : DayGroupAction

    data class MoveTagGroupToPreviousDay(val tagGroup: TagGroup) : DayGroupAction

    data class DeleteTagGroup(val tagGroup: TagGroup) : DayGroupAction

    data class ForceUploadTagGroup(val tagGroup: TagGroup) : DayGroupAction

    data class CopyTagGroup(val tagGroup: TagGroup) : DayGroupAction

    data class Notify(val entry: JournalEntry, val inTime: Duration) : DayGroupAction

    data object CopyDayGroup : DayGroupAction

    data class ResolveConflict(val entry: JournalEntry, val conflict: EntryConflict?) :
        DayGroupAction

    data object ShowAllDays : DayGroupAction

    data class ToggleVerifyEntries(val verify: Boolean) : DayGroupAction

    data object ShowNextDay : DayGroupAction

    data object ShowPreviousDay : DayGroupAction
}
