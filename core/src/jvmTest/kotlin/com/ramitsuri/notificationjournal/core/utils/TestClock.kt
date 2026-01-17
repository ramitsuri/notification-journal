package com.ramitsuri.notificationjournal.core.utils

import kotlin.time.Clock
import kotlin.time.Instant

class TestClock : Clock {
    var now: Instant? = null

    override fun now(): Instant {
        return now!!
    }
}
