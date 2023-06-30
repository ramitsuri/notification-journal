package com.ramitsuri.notificationjournal.core.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatForDisplay(
    toFormat: Instant,
    timeZone: ZoneId
): String {
    return format(toFormat = toFormat, format = "MMM d HH:mm", timeZone = timeZone)
}

private fun format(
    toFormat: Instant,
    format: String,
    timeZone: ZoneId
): String {
    val formatter = DateTimeFormatter
        .ofPattern(format)
        .withLocale(Locale.getDefault())
        .withZone(timeZone)
    return formatter.format(toFormat)
}