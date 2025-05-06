package com.ramitsuri.notificationjournal.core.model

import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry

fun List<JournalEntry>.toDayGroups(tagsForSort: List<String> = listOf()): List<DayGroup> {
    return groupBy {
        it.entryTime.date
    }.map { (date, entriesByDate) ->
        val tagToEntries =
            entriesByDate
                .groupBy { it.tag }
                .map { (tag, entriesByTag) ->
                    val entries = entriesByTag.sortedBy { it.entryTime }
                    tag to entries
                }
                .toMap()

        val tagGroupNoTag =
            entriesByDate
                .filter {
                    Tag.isNoTag(it.tag)
                }
                .takeIf { it.isNotEmpty() }
                ?.sortedBy { it.entryTime }
                ?.let {
                    TagGroup(Tag.NO_TAG.value, it)
                }

        val tagGroupsExistingTags =
            tagsForSort
                .map { tag ->
                    TagGroup(tag, tagToEntries[tag] ?: listOf())
                }

        val tagGroupsRemaining =
            tagToEntries
                .filter { (tag, _) ->
                    !tagsForSort.contains(tag) && !Tag.isNoTag(tag)
                }
                .map { (tag, entries) ->
                    TagGroup(tag, entries)
                }

        val tagGroups =
            buildList {
                if (tagGroupNoTag != null) {
                    add(tagGroupNoTag)
                }
                addAll(tagGroupsExistingTags)
                addAll(tagGroupsRemaining)
            }
        DayGroup(date, tagGroups)
    }
}
