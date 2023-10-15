package com.ramitsuri.notificationjournal.core.utils

import com.ramitsuri.notificationjournal.core.text.LocalizedString
import com.ramitsuri.notificationjournal.core.text.TextValue
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
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
    toFormat: LocalDate,
    now: Instant = Instant.now(),
    timeZone: ZoneId = ZoneId.systemDefault(),
): TextValue {
    val nowLocalDateTime = getLocalDate(now, timeZone).atTime(0, 0)
    val toFormatDateTime = toFormat.atTime(0, 0)
    val daysBetweenNowAndToFormat = Duration.between(
        nowLocalDateTime.truncatedTo(ChronoUnit.DAYS),
        toFormatDateTime.truncatedTo(ChronoUnit.DAYS)
    ).toDays()
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
            TextValue.ForString(format(toFormatDateTime, "MMM d", timeZone))
        }
    }
}

fun getLocalDate(
    time: Instant,
    zoneId: ZoneId = ZoneId.systemDefault()
): LocalDate {
    return time.atZone(zoneId).toLocalDate()
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

private fun format(
    toFormat: LocalDateTime,
    format: String,
    timeZone: ZoneId
): String {
    val formatter = DateTimeFormatter
        .ofPattern(format)
        .withLocale(Locale.getDefault())
        .withZone(timeZone)
    return formatter.format(toFormat)
}