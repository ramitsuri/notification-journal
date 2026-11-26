package com.ramitsuri.notificationjournal.core.model

import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class ReconcileCheckTest {
    @Test
    fun `summary formats ANY_ENTRY for every day`() {
        val check =
            ReconcileCheck(
                id = "check-1",
                tag = "Feeling",
                daysOfWeek = DayOfWeek.entries.toSet(),
                matchType = ReconcileCheckMatchType.ANY_ENTRY,
                query = "",
            )

        assertEquals(
            "Every day • Feeling • at least 1 entry",
            check.summary(),
        )
    }

    @Test
    fun `summary formats subset of days in canonical Mon-Sun order`() {
        val check =
            ReconcileCheck(
                id = "check-2",
                tag = "Activity",
                daysOfWeek = setOf(DayOfWeek.THURSDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
                matchType = ReconcileCheckMatchType.ANY_ENTRY,
                query = "",
            )

        assertEquals(
            "Tue, Wed, Thu • Activity • at least 1 entry",
            check.summary(),
        )
    }

    @Test
    fun `summary formats STARTS_WITH with trimmed query`() {
        val check =
            ReconcileCheck(
                id = "check-3",
                tag = "Health",
                daysOfWeek = DayOfWeek.entries.toSet(),
                matchType = ReconcileCheckMatchType.STARTS_WITH,
                query = "  Woke up at  ",
            )

        assertEquals(
            "Every day • Health • starts with \"Woke up at\"",
            check.summary(),
        )
    }

    @Test
    fun `summary formats CONTAINS_ALL_WORDS splitting on whitespace and commas`() {
        val check =
            ReconcileCheck(
                id = "check-4",
                tag = "Activity",
                daysOfWeek = setOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
                matchType = ReconcileCheckMatchType.CONTAINS_ALL_WORDS,
                query = " to,  work , ",
            )

        assertEquals(
            "Tue, Wed, Thu • Activity • contains \"to\", \"work\"",
            check.summary(),
        )
    }

    @Test
    fun `summary formats remaining days of week`() {
        val check =
            ReconcileCheck(
                id = "check-5",
                tag = "Journal",
                daysOfWeek = setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.SATURDAY, DayOfWeek.FRIDAY),
                matchType = ReconcileCheckMatchType.ANY_ENTRY,
                query = "",
            )

        assertEquals(
            "Mon, Fri, Sat, Sun • Journal • at least 1 entry",
            check.summary(),
        )
    }
}
