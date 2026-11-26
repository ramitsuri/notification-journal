package com.ramitsuri.notificationjournal.core.utils

import com.ramitsuri.notificationjournal.core.model.ReconcileCheckMatchType
import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseConvertersTest {
    private val converters = DatabaseConverters()

    @Test
    fun `daysOfWeek round-trips single subset and full week`() {
        val single = setOf(DayOfWeek.MONDAY)
        assertEquals(single, converters.toDaysOfWeek(converters.fromDaysOfWeek(single)))

        val subset = setOf(DayOfWeek.THURSDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY)
        val encodedSubset = converters.fromDaysOfWeek(subset)
        assertEquals("TUESDAY,WEDNESDAY,THURSDAY", encodedSubset)
        assertEquals(subset, converters.toDaysOfWeek(encodedSubset))

        val allDays = DayOfWeek.entries.toSet()
        assertEquals(allDays, converters.toDaysOfWeek(converters.fromDaysOfWeek(allDays)))
    }

    @Test
    fun `toDaysOfWeek handles blank and unknown tokens safely`() {
        assertEquals(emptySet<DayOfWeek>(), converters.toDaysOfWeek(""))
        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            converters.toDaysOfWeek(" MONDAY , INVALID , FRIDAY "),
        )
    }

    @Test
    fun `reconcileCheckMatchType round-trips all enum values and falls back on unknown`() {
        ReconcileCheckMatchType.entries.forEach { matchType ->
            val encoded = converters.fromReconcileCheckMatchType(matchType)
            assertEquals(matchType.name, encoded)
            assertEquals(matchType, converters.toReconcileCheckMatchType(encoded))
        }
        assertEquals(
            ReconcileCheckMatchType.ANY_ENTRY,
            converters.toReconcileCheckMatchType("UNKNOWN_VALUE"),
        )
    }
}
