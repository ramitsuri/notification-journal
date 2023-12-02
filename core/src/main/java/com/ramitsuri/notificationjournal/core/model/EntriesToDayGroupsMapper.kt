package com.ramitsuri.notificationjournal.core.model

import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.utils.getLocalDate
import java.time.ZoneId

fun List<JournalEntry>.toDayGroups(
    zoneId: ZoneId = ZoneId.systemDefault(),
    tagsForSort: List<Tag> = listOf(),
): List<DayGroup> {
    val tags = listOf(Tag.NO_TAG) + tagsForSort
    return groupBy {
        val entryTime = it.entryTimeOverride ?: it.entryTime
        getLocalDate(entryTime, zoneId)
    }.map { (date, entriesByDate) ->
        val byTag = entriesByDate
            .groupBy { it.tag }
            .map { (tag, entriesByTag) ->
                val nonNullTag = tag ?: Tag.NO_TAG.value
                val entries = entriesByTag.sortedBy { it.entryTimeOverride ?: it.entryTime }
                TagGroup(nonNullTag, entries)
            }
            .sortedBy { (tag, _) ->
                tags.firstOrNull { it.value == tag }?.order
            }
        DayGroup(date, byTag)
    }
}