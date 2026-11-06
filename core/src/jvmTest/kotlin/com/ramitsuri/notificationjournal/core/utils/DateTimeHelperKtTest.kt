package com.ramitsuri.notificationjournal.core.utils

import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DateTimeHelperKtTest {
    private var toFormat: String = ""

    @Test
    fun testFormatForDisplay() {
        toFormat = "2024-07-18T01:00:00"
        assertFormattedEquals("1am")

        toFormat = "2024-07-18T13:00:00"
        assertFormattedEquals("1pm")

        toFormat = "2024-07-18T23:00:00"
        assertFormattedEquals("11pm")

        toFormat = "2024-07-18T00:00:00"
        assertFormattedEquals("12am")

        toFormat = "2024-07-18T00:30:00"
        assertFormattedEquals("12:30am")

        toFormat = "2024-07-18T00:30:59"
        assertFormattedEquals("12:30am")

        toFormat = "2024-07-18T16:30:59"
        assertFormattedEquals("4:30pm")
    }

    @Test
    fun testParseLocalTime() {
        assertEquals(kotlinx.datetime.LocalTime(20, 0), parseLocalTime("20:00"))
        assertEquals(kotlinx.datetime.LocalTime(8, 30), parseLocalTime("08:30"))
        assertEquals(kotlinx.datetime.LocalTime(8, 30), parseLocalTime("8:30"))
        assertEquals(kotlinx.datetime.LocalTime(0, 0), parseLocalTime("00:00"))
        assertEquals(kotlinx.datetime.LocalTime(23, 59), parseLocalTime("23:59"))
        org.junit.Assert.assertNull(parseLocalTime("not_a_time"))
        org.junit.Assert.assertNull(parseLocalTime(""))
    }

    @Test
    fun testResolveEntryTime() {
        val fallback = LocalDateTime.parse("2026-11-06T15:00:00")

        // No target date or time -> fallback
        assertEquals(fallback, resolveEntryTime(null, null, fallback))

        // Target time only -> fallback date with target time
        assertEquals(
            LocalDateTime.parse("2026-11-06T20:00:00"),
            resolveEntryTime(null, "20:00", fallback),
        )

        // Target date only -> target date with fallback time
        assertEquals(
            LocalDateTime.parse("2026-11-05T15:00:00"),
            resolveEntryTime("2026-11-05", null, fallback),
        )

        // Both target date and time
        assertEquals(
            LocalDateTime.parse("2026-11-05T20:30:00"),
            resolveEntryTime("2026-11-05", "20:30", fallback),
        )

        // Invalid strings fallback gracefully
        assertEquals(
            fallback,
            resolveEntryTime("invalid-date", "invalid-time", fallback),
        )
    }

    private fun assertFormattedEquals(expected: String) {
        assertEquals(expected, format())
    }

    private fun String.toLocalDateTime(): LocalDateTime = LocalDateTime.parse(this)

    private fun format(): String =
        hourMinute(
            toFormat = toFormat.toLocalDateTime(),
            amString = "am",
            pmString = "pm",
        )
}
