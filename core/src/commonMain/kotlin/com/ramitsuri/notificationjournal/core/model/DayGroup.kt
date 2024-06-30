package com.ramitsuri.notificationjournal.core.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DayGroup(
    @SerialName("date")
    val date: LocalDate,

    @SerialName("tag_groups")
    val tagGroups: List<TagGroup>
) {
    val untaggedCount: Int
        get() = tagGroups
            .firstOrNull {
                it.tag == Tag.NO_TAG.value
            }
            ?.entries
            ?.size
            ?: 0
}
