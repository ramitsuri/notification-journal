package com.ramitsuri.notificationjournal.core.model

import com.squareup.moshi.Json
import java.time.LocalDate

data class DayGroup(
    @Json(name = "date")
    val date: LocalDate,

    @Json(name = "tag_groups")
    val tagGroups: List<TagGroup>
)
