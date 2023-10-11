package com.ramitsuri.notificationjournal.core.utils

import com.ramitsuri.notificationjournal.core.text.LocalizedString
import com.ramitsuri.notificationjournal.core.text.TextValue
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

fun formatForDisplay(
    toFormat: Instant,
    timeZone: ZoneId
): String {
    return format(toFormat = toFormat, format = "MMM d HH:mm", timeZone = timeZone)
}

@Suppress("MoveVariableDeclarationIntoWhen")
fun getDay(
    toFormat: Instant,
    now: Instant = Instant.now(),
    timeZone: ZoneId = ZoneId.systemDefault(),
): TextValue {
    val nowTruncated = now.truncatedTo(ChronoUnit.DAYS)
    val toFormatTruncated = toFormat.truncatedTo(ChronoUnit.DAYS)
    val daysBetweenNowAndToFormat = Duration.between(nowTruncated, toFormatTruncated).toDays()
    return when (daysBetweenNowAndToFormat) {
        0L -> {
            TextValue.ForKey(LocalizedString.TODAY)
        }

        1L -> {
            TextValue.ForKey(LocalizedString.TOMORROW)
        }

        -1L -> {
            TextValue.ForKey(LocalizedString.YESTERDAY)
        }

        else -> {
            TextValue.ForString(format(toFormat, "MMM d", timeZone))
        }
    }
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