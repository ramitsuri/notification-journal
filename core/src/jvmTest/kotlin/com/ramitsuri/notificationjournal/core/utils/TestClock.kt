package com.ramitsuri.notificationjournal.core.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class TestClock : Clock {
    var now: Instant? = null

    override fun now(): Instant {
        return now!!
    }
}
