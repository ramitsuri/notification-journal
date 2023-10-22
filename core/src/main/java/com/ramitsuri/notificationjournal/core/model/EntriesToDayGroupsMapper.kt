package com.ramitsuri.notificationjournal.core.model

import com.ramitsuri.notificationjournal.core.utils.getLocalDate
import java.time.ZoneId

fun List<JournalEntry>.toDayGroups(
    zoneId: ZoneId = ZoneId.systemDefault(),
    tagsForSort: List<Tag> = listOf()
): List<DayGroup> {
    return groupBy {
        val entryTime = it.entryTimeOverride ?: it.entryTime
        getLocalDate(entryTime, zoneId)
    }.map { (date, entriesByDate) ->
        val byTag = entriesByDate
            .groupBy { it.tag }
            .map { (tag, entriesByTag) ->
                TagGroup(tag, entriesByTag.sortedBy { it.entryTimeOverride ?: it.entryTime })
            }
        val sorted = if (tagsForSort.isNotEmpty()) {
            byTag.sortedBy { (tag, _) ->
                tagsForSort.first { it.value == tag }.order
            }
        } else {
            byTag
        }
        DayGroup(date, sorted)
    }
}