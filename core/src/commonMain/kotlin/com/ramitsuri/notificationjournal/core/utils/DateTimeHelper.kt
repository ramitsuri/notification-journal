package com.ramitsuri.notificationjournal.core.utils

import androidx.compose.runtime.Composable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.am
import notificationjournal.core.generated.resources.day_of_week_names
import notificationjournal.core.generated.resources.month_names
import notificationjournal.core.generated.resources.month_names_short
import notificationjournal.core.generated.resources.pm
import notificationjournal.core.generated.resources.today
import notificationjournal.core.generated.resources.tomorrow
import notificationjournal.core.generated.resources.yesterday
import org.jetbrains.compose.resources.getStringArray
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration

// 4:45pm, 4pm
fun hourMinute(
    toFormat: LocalDateTime,
    amString: String,
    pmString: String,
): String {
    val minute = toFormat.minute
    val format =
        LocalDateTime.Format {
            amPmHour(padding = Padding.NONE)
            if (minute != 0) {
                char(':')
                minute()
            }
            amPmMarker(am = amString, pm = pmString)
        }
    return toFormat.format(format)
}

fun formatTimeForLogs(
    toFormat: Instant,
    timeZone: TimeZone,
    amString: String,
    pmString: String,
): String {
    val localDateTime = toFormat.toLocalDateTime(timeZone)
    val format =
        LocalDateTime.Format {
            amPmHour(padding = Padding.NONE)
            char(':')
            minute()
            char(':')
            second()
            char('.')
            secondFraction(maxLength = 3)
            char(' ')
            amPmMarker(am = amString, pm = pmString)
        }
    return localDateTime.format(format)
}

@Composable
fun monthDayHourMinute(
    toFormat: LocalDateTime,
    monthNames: List<String> = stringArrayResource(Res.array.month_names_short),
    amString: String = stringResource(Res.string.am),
    pmString: String = stringResource(Res.string.pm),
): String {
    val minute = toFormat.minute
    val format =
        LocalDateTime.Format {
            monthName(MonthNames(monthNames))
            char(' ')
            dayOfMonth()
            char(',')
            char(' ')
            amPmHour(padding = Padding.NONE)
            if (minute != 0) {
                char(':')
                minute()
            }
            amPmMarker(am = amString, pm = pmString)
        }
    return toFormat.format(format)
}

@Suppress("MoveVariableDeclarationIntoWhen")
@Composable
fun dayMonthDate(
    toFormat: LocalDate,
    now: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    monthNames: List<String> = stringArrayResource(Res.array.month_names),
    dayOfWeekNames: List<String> = stringArrayResource(Res.array.day_of_week_names),
): String {
    val nowLocalDate = now.toLocalDateTime(timeZone).date
    val daysBetweenNowAndToFormat = toFormat.daysUntil(nowLocalDate)
    return when (daysBetweenNowAndToFormat) {
        0 -> {
            stringResource(Res.string.today)
        }

        1 -> {
            stringResource(Res.string.yesterday)
        }

        -1 -> {
            stringResource(Res.string.tomorrow)
        }

        else -> {
            val format =
                LocalDateTime.Format {
                    dayOfWeek(DayOfWeekNames(dayOfWeekNames))
                    char(',')
                    char(' ')
                    monthName(MonthNames(monthNames))
                    char(' ')
                    dayOfMonth()
                    if (nowLocalDate.year != toFormat.year) {
                        char(' ')
                        year()
                    }
                }
            return toFormat
                .atTime(hour = 0, minute = 0)
                .format(format)
        }
    }
}

// 4:45pm
@Composable
fun hourMinute(
    toFormat: LocalTime,
    amString: String = stringResource(Res.string.am),
    pmString: String = stringResource(Res.string.pm),
): String {
    val format =
        LocalTime.Format {
            amPmHour(padding = Padding.NONE)
            char(':')
            minute()
            amPmMarker(am = amString, pm = pmString)
        }
    return toFormat.format(format)
}

// Saturday, November 16 2024
suspend fun dayMonthDateWithYearSuspend(toFormat: LocalDate): String {
    val monthNames = getStringArray(Res.array.month_names)
    val dayOfWeekNames = getStringArray(Res.array.day_of_week_names)
    return dayMonthDateWithYearInternal(
        toFormat = toFormat,
        monthNames = monthNames,
        dayOfWeekNames = dayOfWeekNames,
    )
}

// Saturday, November 16 2024
@Composable
fun dayMonthDateWithYear(
    toFormat: LocalDate,
    monthNames: List<String> = stringArrayResource(Res.array.month_names),
    dayOfWeekNames: List<String> = stringArrayResource(Res.array.day_of_week_names),
    dayDateSeparatorUsesNewLine: Boolean = false,
): String {
    return dayMonthDateWithYearInternal(
        toFormat = toFormat,
        monthNames = monthNames,
        dayOfWeekNames = dayOfWeekNames,
        dayDateSeparatorUsesNewLine = dayDateSeparatorUsesNewLine,
    )
}

fun LocalDateTime.plus(duration: Duration) =
    toInstant(TimeZone.UTC)
        .plus(duration)
        .toLocalDateTime(TimeZone.UTC)

fun LocalDateTime.minus(duration: Duration) =
    toInstant(TimeZone.UTC)
        .minus(duration)
        .toLocalDateTime(TimeZone.UTC)

fun Clock.nowLocal(): LocalDateTime = now().toLocalDateTime(TimeZone.currentSystemDefault())

// Formats the date in the following format:
// 2024/12/31.md
fun LocalDate.asImportFileName() =
    buildString {
        append(year)
        append("/")
        append(String.format("%02d", month.value))
        append("/")
        append(String.format("%02d", dayOfMonth))
        append(".md")
    }

private fun dayMonthDateWithYearInternal(
    toFormat: LocalDate,
    monthNames: List<String>,
    dayOfWeekNames: List<String>,
    dayDateSeparatorUsesNewLine: Boolean = false,
): String {
    return LocalDate.Format {
        dayOfWeek(DayOfWeekNames(dayOfWeekNames))
        if (dayDateSeparatorUsesNewLine) {
            char('\n')
        } else {
            char(',')
            char(' ')
        }
        monthName(MonthNames(monthNames))
        char(' ')
        dayOfMonth()
        char(' ')
        year()
    }.format(toFormat)
}
