package com.ramitsuri.notificationjournal.core.model

import java.time.LocalDate

data class DayGroup(val date: LocalDate, val tagGroups: List<TagGroup>)
