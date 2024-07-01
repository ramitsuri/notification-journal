package com.ramitsuri.notificationjournal.core.utils

import androidx.compose.runtime.Composable
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
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.day_of_week_names
import notificationjournal.core.generated.resources.month_names
import notificationjournal.core.generated.resources.today
import notificationjournal.core.generated.resources.tomorrow
import notificationjournal.core.generated.resources.yesterday
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

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
@Composable
fun getDay(
    toFormat: LocalDate,
    now: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    monthNames: List<String> = stringArrayResource(Res.array.month_names),
    dayOfWeekNames: List<String> = stringArrayResource(Res.array.day_of_week_names),
): String {
    val nowLocalDate = now.toLocalDateTime(timeZone).date
    val daysBetweenNowAndToFormat = nowLocalDate.minus(toFormat).days
    return when (daysBetweenNowAndToFormat) {
        0 -> {
            stringResource(Res.string.today)
        }

        1 -> {
            stringResource(Res.string.tomorrow)
        }

        -1 -> {
            stringResource(Res.string.yesterday)
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
            return toFormat
                .atTime(hour = 0, minute = 0)
                .format(format)
        }
    }
}

fun getLocalDate(
    time: Instant,
    zoneId: TimeZone = TimeZone.currentSystemDefault(),
): LocalDate {
    return time.toLocalDateTime(zoneId).date
}