package com.ramitsuri.notificationjournal.core.model

import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.notify_fifteen_minutes
import notificationjournal.core.generated.resources.notify_now
import notificationjournal.core.generated.resources.notify_one_hour
import notificationjournal.core.generated.resources.notify_six_hours
import notificationjournal.core.generated.resources.notify_twelve_hours
import notificationjournal.core.generated.resources.notify_twenty_four_hours
import notificationjournal.core.generated.resources.notify_two_hours
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

enum class NotifyTime(
    val duration: Duration,
    val res: StringResource,
) {
    NOW(
        duration = Duration.ZERO,
        res = Res.string.notify_now,
    ),

    FIFTEEN_MINUTES(
        duration = 15.minutes,
        res = Res.string.notify_fifteen_minutes,
    ),

    ONE_HOUR(
        duration = 1.hours,
        res = Res.string.notify_one_hour,
    ),

    TWO_HOURS(
        duration = 2.hours,
        res = Res.string.notify_two_hours,
    ),

    SIX_HOURS(
        duration = 6.hours,
        res = Res.string.notify_six_hours,
    ),

    TWELVE_HOURS(
        duration = 12.hours,
        res = Res.string.notify_twelve_hours,
    ),

    TWENTY_FOUR_HOURS(
        duration = 24.hours,
        res = Res.string.notify_twenty_four_hours,
    ),
}
