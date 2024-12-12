package com.ramitsuri.notificationjournal.core.log

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class LogData(
    val time: Instant = Clock.System.now(),
    val message: String,
    val tag: String,
    val errorMessage: String?,
    val stackTrace: String?,
)
