package com.ramitsuri.notificationjournal.core.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Test

class DateTimeHelperKtTest {
    private val timeZone = TimeZone.of("America/New_York")
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

    private fun assertFormattedEquals(expected: String) {
        assertEquals(expected, format())
    }

    private fun String.toInstant(): Instant {
        return LocalDateTime.parse(this).toInstant(timeZone)
    }

    private fun format(): String {
        return formatForDisplay(
            toFormat = toFormat.toInstant(),
            timeZone = timeZone,
            amString = "am",
            pmString = "pm",
        )
    }
}