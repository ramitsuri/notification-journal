package com.ramitsuri.notificationjournal.core.model

import com.ramitsuri.notificationjournal.core.utils.getLocalDate
import java.time.ZoneId

fun List<JournalEntry>.toDayGroups(zoneId: ZoneId = ZoneId.systemDefault()): List<DayGroup> {
    return groupBy { getLocalDate(it.entryTime, zoneId) }
        .map { (date, entriesByDate) ->
            val byTag = entriesByDate
                .groupBy { it.tag }
                .map { (tag, entriesByTag) ->
                    TagGroup(tag, entriesByTag)
                }
            DayGroup(date, byTag)
        }
}