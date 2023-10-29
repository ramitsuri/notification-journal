package com.ramitsuri.notificationjournal.core.model

import com.ramitsuri.notificationjournal.core.utils.getLocalDate
import java.time.ZoneId

fun List<JournalEntry>.toDayGroups(
    zoneId: ZoneId = ZoneId.systemDefault(),
    tagsForSort: List<Tag> = listOf(),
    sortByEntryTime: Boolean = false,
    sortByTagOrder: Boolean = false,
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
                val entries = if (sortByEntryTime) {
                    entriesByTag.sortedBy { it.entryTimeOverride ?: it.entryTime }
                } else {
                    entriesByTag
                }
                TagGroup(nonNullTag, entries)
            }
        val sorted = if (sortByTagOrder) {
            byTag.sortedBy { (tag, _) ->
                tags.first { it.value == tag }.order
            }
        } else {
            byTag
        }
        DayGroup(date, sorted)
    }
}