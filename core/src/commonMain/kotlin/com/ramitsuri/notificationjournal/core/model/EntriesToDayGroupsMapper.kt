package com.ramitsuri.notificationjournal.core.model

import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.utils.getLocalDate
import kotlinx.datetime.TimeZone

fun List<JournalEntry>.toDayGroups(
    zoneId: TimeZone = TimeZone.currentSystemDefault(),
    tagsForSort: List<Tag> = listOf(),
): List<DayGroup> {
    val tags = listOf(Tag.NO_TAG) + tagsForSort
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
        val tagGroups = if (tags.isEmpty()) {
            tagToEntries.map { (tag, entries) -> TagGroup(tag, entries) }
        } else {
            tags
                .mapNotNull { tag ->
                    val entriesForTag = tagToEntries[tag.value] ?: listOf()
                    if (tag.value == Tag.NO_TAG.value && entriesForTag.isEmpty()) {
                        null
                    } else {
                        TagGroup(tag.value, entriesForTag)
                    }
                }
        }
        DayGroup(date, tagGroups)
    }
}