package com.ramitsuri.notificationjournal.core.model.logs

import com.ramitsuri.notificationjournal.core.log.LogData
import kotlinx.datetime.TimeZone

data class LogsViewState(
    val timeZone: TimeZone,
    val logs: List<LogData> = emptyList(),
    val tags: List<Tag> = emptyList(),
) {
    data class Tag(
        val value: String,
        val selected: Boolean = false,
    )
}
