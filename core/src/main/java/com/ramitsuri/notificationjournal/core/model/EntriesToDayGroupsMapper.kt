package com.ramitsuri.notificationjournal.core.model

import com.ramitsuri.notificationjournal.core.utils.getLocalDate
import java.time.ZoneId

fun List<JournalEntry>.toDayGroups(
    zoneId: ZoneId = ZoneId.systemDefault(),
    tagsForSort: List<Tag> = listOf(),
    sortByEntryTime: Boolean = false,
    sortByTagOrder: Boolean = false,
): List<DayGroup> {
    return groupBy {
        val entryTime = it.entryTimeOverride ?: it.entryTime
        getLocalDate(entryTime, zoneId)
    }.map { (date, entriesByDate) ->
        val byTag = entriesByDate
            .groupBy { it.tag }
            .map { (tag, entriesByTag) ->
                if (sortByEntryTime) {
                    TagGroup(tag, entriesByTag.sortedBy { it.entryTimeOverride ?: it.entryTime })
                } else {
                    TagGroup(tag, entriesByTag)
                }
            }
        val sorted = if (tagsForSort.isNotEmpty() && sortByTagOrder) {
            byTag.sortedBy { (tag, _) ->
                tagsForSort.first { it.value == tag }.order
            }
        } else {
            byTag
        }
        DayGroup(date, sorted)
    }
}