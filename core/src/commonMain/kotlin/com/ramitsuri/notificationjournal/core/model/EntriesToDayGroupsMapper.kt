package com.ramitsuri.notificationjournal.core.model

import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.utils.getLocalDate
import kotlinx.datetime.TimeZone

fun List<JournalEntry>.toDayGroups(
    zoneId: TimeZone = TimeZone.currentSystemDefault(),
    tagsForSort: List<Tag> = listOf(),
): List<DayGroup> {
    return groupBy {
        val entryTime = it.entryTime
        getLocalDate(entryTime, zoneId)
    }.map { (date, entriesByDate) ->
        val tagToEntries = entriesByDate
            .groupBy { it.tag }
            .map { (tag, entriesByTag) ->
                val nonNullTag = tag ?: Tag.NO_TAG.value
                val entries = entriesByTag.sortedBy { it.entryTime }
                nonNullTag to entries
            }
            .toMap()
        val tagGroups = if (tagsForSort.isEmpty()) {
            tagToEntries.map { (tag, entries) -> TagGroup(tag, entries) }
        } else {
            tagsForSort.map { tag ->
                TagGroup(tag.value, tagToEntries[tag.value] ?: listOf())
            }
        }
        val noTagEntries = entriesByDate.filter { Tag.isNoTag(it.tag) }
        if (noTagEntries.isEmpty()) {
            DayGroup(date, tagGroups)
        } else {
            DayGroup(date, listOf(TagGroup(Tag.NO_TAG.value, noTagEntries)) + tagGroups)
        }
    }
}