package com.ramitsuri.notificationjournal.core.utils

import com.ramitsuri.notificationjournal.core.text.LocalizedString
import com.ramitsuri.notificationjournal.core.text.TextValue
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

fun formatForDisplay(
    toFormat: Instant,
    timeZone: TimeZone,
    amString: String,
    pmString: String,
): String {
    val format = LocalDateTime.Format {
        amPmHour()
        char(':')
        minute()
        char(' ')
        amPmMarker(am = amString, pm = pmString)
    }
    return toFormat
        .toLocalDateTime(timeZone)
        .format(format)
}

@Suppress("MoveVariableDeclarationIntoWhen")
fun getDay(
    toFormat: LocalDate,
    now: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    monthNames: List<String>,
    dayOfWeekNames: List<String>,
): TextValue {
    val nowLocalDate = now.toLocalDateTime(timeZone).date
    val daysBetweenNowAndToFormat = nowLocalDate.minus(toFormat).days
    return when (daysBetweenNowAndToFormat) {
        0 -> {
            TextValue.ForKey(LocalizedString.TODAY)
        }

        1 -> {
            TextValue.ForKey(LocalizedString.YESTERDAY)
        }

        -1 -> {
            TextValue.ForKey(LocalizedString.TOMORROW)
        }

        else -> {
            val format = LocalDateTime.Format {
                dayOfWeek(DayOfWeekNames(dayOfWeekNames))
                char(',')
                char(' ')
                monthName(MonthNames(monthNames))
                char(' ')
                dayOfMonth()
            }
            return TextValue.ForString(
                toFormat
                    .atTime(hour = 0, minute = 0)
                    .format(format)
            )
        }
    }
}

fun getLocalDate(
    time: Instant,
    zoneId: TimeZone = TimeZone.currentSystemDefault(),
): LocalDate {
    return time.toLocalDateTime(zoneId).date
}