package com.ramitsuri.notificationjournal.core.log

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class LogData
    @OptIn(ExperimentalTime::class)
    constructor(
        val time: Instant = Clock.System.now(),
        val message: String,
        val tag: String,
        val errorMessage: String?,
        val stackTrace: String?,
    )
