package com.ramitsuri.notificationjournal.core.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.LocalDate

@JsonClass(generateAdapter = true)
data class DayGroup(
    @Json(name = "date")
    val date: LocalDate,

    @Json(name = "tag_groups")
    val tagGroups: List<TagGroup>
)
