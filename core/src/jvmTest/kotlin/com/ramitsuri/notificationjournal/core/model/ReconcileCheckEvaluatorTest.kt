package com.ramitsuri.notificationjournal.core.model

import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReconcileCheckEvaluatorTest {
    @Test
    fun `ANY_ENTRY fails when tag is missing and passes when tag has an entry`() {
        // 2026-11-25 is Wednesday
        val date = LocalDate.parse("2026-11-25")
        val dayGroupMissingTag =
            DayGroup(
                date = date,
                tagGroups =
                    listOf(
                        TagGroup(
                            tag = "Other",
                            entries = listOf(entry(date = date, tag = "Other", text = "hello")),
                        ),
                    ),
            )
        val check =
            check(
                id = "c1",
                tag = "Feeling",
                matchType = ReconcileCheckMatchType.ANY_ENTRY,
            )

        assertEquals(
            listOf(check),
            ReconcileCheckEvaluator.evaluate(dayGroupMissingTag, listOf(check)),
        )

        val dayGroupWithTag =
            DayGroup(
                date = date,
                tagGroups =
                    listOf(
                        TagGroup(
                            tag = "Feeling",
                            entries = listOf(entry(date = date, tag = "Feeling", text = "calm")),
                        ),
                    ),
            )

        assertEquals(
            emptyList<ReconcileCheck>(),
            ReconcileCheckEvaluator.evaluate(dayGroupWithTag, listOf(check)),
        )
    }

    @Test
    fun `disabled check is skipped`() {
        val date = LocalDate.parse("2026-11-25")
        val emptyDayGroup = DayGroup(date = date, tagGroups = emptyList())
        val disabledCheck =
            check(
                id = "c-disabled",
                tag = "Feeling",
                enabled = false,
            )

        assertEquals(
            emptyList<ReconcileCheck>(),
            ReconcileCheckEvaluator.evaluate(emptyDayGroup, listOf(disabledCheck)),
        )
    }

    @Test
    fun `check is skipped on non-matching day of week and evaluated on matching day`() {
        // 2026-11-25 is Wednesday; 2026-11-26 is Thursday
        val wednesday = LocalDate.parse("2026-11-25")
        val thursday = LocalDate.parse("2026-11-26")
        val tuesdayThursdayCheck =
            check(
                id = "c-days",
                tag = "Activity",
                daysOfWeek = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
            )

        assertEquals(
            emptyList<ReconcileCheck>(),
            ReconcileCheckEvaluator.evaluate(
                DayGroup(date = wednesday, tagGroups = emptyList()),
                listOf(tuesdayThursdayCheck),
            ),
        )
        assertEquals(
            listOf(tuesdayThursdayCheck),
            ReconcileCheckEvaluator.evaluate(
                DayGroup(date = thursday, tagGroups = emptyList()),
                listOf(tuesdayThursdayCheck),
            ),
        )
    }

    @Test
    fun `STARTS_WITH matches trimmed entry text case-insensitively and fails when absent`() {
        val date = LocalDate.parse("2026-11-25")
        val check =
            check(
                id = "c-starts",
                tag = "Health",
                matchType = ReconcileCheckMatchType.STARTS_WITH,
                query = "  Woke up at ",
            )
        val nonMatchingDay =
            DayGroup(
                date = date,
                tagGroups =
                    listOf(
                        TagGroup(
                            tag = "Health",
                            entries = listOf(entry(date = date, tag = "Health", text = "Later woke up at 7am")),
                        ),
                    ),
            )
        val matchingDay =
            DayGroup(
                date = date,
                tagGroups =
                    listOf(
                        TagGroup(
                            tag = "Health",
                            entries =
                                listOf(
                                    entry(date = date, tag = "Health", text = "Went for a walk"),
                                    entry(date = date, tag = "Health", text = "  woke up at 6:30am"),
                                ),
                        ),
                    ),
            )

        assertEquals(listOf(check), ReconcileCheckEvaluator.evaluate(nonMatchingDay, listOf(check)))
        assertEquals(emptyList<ReconcileCheck>(), ReconcileCheckEvaluator.evaluate(matchingDay, listOf(check)))
    }

    @Test
    fun `CONTAINS_ALL_WORDS requires all words as whole words in a single entry`() {
        val date = LocalDate.parse("2026-11-25")
        val check =
            check(
                id = "c-words",
                tag = "Activity",
                matchType = ReconcileCheckMatchType.CONTAINS_ALL_WORDS,
                query = "to, work",
            )
        val substringOnlyDay =
            DayGroup(
                date = date,
                tagGroups =
                    listOf(
                        TagGroup(
                            tag = "Activity",
                            entries = listOf(entry(date = date, tag = "Activity", text = "Today at work")),
                        ),
                    ),
            )
        val splitAcrossEntriesDay =
            DayGroup(
                date = date,
                tagGroups =
                    listOf(
                        TagGroup(
                            tag = "Activity",
                            entries =
                                listOf(
                                    entry(date = date, tag = "Activity", text = "Walked to store"),
                                    entry(date = date, tag = "Activity", text = "Started work"),
                                ),
                        ),
                    ),
            )
        val singleEntryMatchDay =
            DayGroup(
                date = date,
                tagGroups =
                    listOf(
                        TagGroup(
                            tag = "Activity",
                            entries = listOf(entry(date = date, tag = "Activity", text = "Drove TO work.")),
                        ),
                    ),
            )

        assertEquals(listOf(check), ReconcileCheckEvaluator.evaluate(substringOnlyDay, listOf(check)))
        assertEquals(listOf(check), ReconcileCheckEvaluator.evaluate(splitAcrossEntriesDay, listOf(check)))
        assertEquals(emptyList<ReconcileCheck>(), ReconcileCheckEvaluator.evaluate(singleEntryMatchDay, listOf(check)))
    }

    @Test
    fun `CONTAINS_ALL_WORDS matches whole words bounded by start, end, and punctuation`() {
        val date = LocalDate.parse("2026-11-25")
        val check =
            check(
                id = "c-boundary",
                tag = "Activity",
                matchType = ReconcileCheckMatchType.CONTAINS_ALL_WORDS,
                query = "to, work",
            )
        val startAndEndDay =
            DayGroup(
                date = date,
                tagGroups =
                    listOf(
                        TagGroup(
                            tag = "Activity",
                            entries = listOf(entry(date = date, tag = "Activity", text = "to-do: work")),
                        ),
                    ),
            )
        val prefixedSuffixDay =
            DayGroup(
                date = date,
                tagGroups =
                    listOf(
                        TagGroup(
                            tag = "Activity",
                            entries = listOf(entry(date = date, tag = "Activity", text = "into working")),
                        ),
                    ),
            )

        assertEquals(emptyList<ReconcileCheck>(), ReconcileCheckEvaluator.evaluate(startAndEndDay, listOf(check)))
        assertEquals(listOf(check), ReconcileCheckEvaluator.evaluate(prefixedSuffixDay, listOf(check)))
    }

    @Test
    fun `ANY_ENTRY fails when TagGroup exists with empty entries`() {
        val date = LocalDate.parse("2026-11-25")
        val dayGroup =
            DayGroup(
                date = date,
                tagGroups = listOf(TagGroup(tag = "Feeling", entries = emptyList())),
            )
        val check = check(id = "c-empty", tag = "Feeling", matchType = ReconcileCheckMatchType.ANY_ENTRY)

        assertEquals(listOf(check), ReconcileCheckEvaluator.evaluate(dayGroup, listOf(check)))
    }

    @Test
    fun `evaluate returns failing checks in input order`() {
        val date = LocalDate.parse("2026-11-25")
        val dayGroup =
            DayGroup(
                date = date,
                tagGroups =
                    listOf(
                        TagGroup(
                            tag = "Feeling",
                            entries = listOf(entry(date = date, tag = "Feeling", text = "Tired")),
                        ),
                    ),
            )
        val passingCheck = check(id = "c1", tag = "Feeling", matchType = ReconcileCheckMatchType.ANY_ENTRY)
        val failingCheck1 =
            check(
                id = "c2",
                tag = "Health",
                matchType = ReconcileCheckMatchType.STARTS_WITH,
                query = "Woke up",
            )
        val failingCheck2 =
            check(
                id = "c3",
                tag = "Activity",
                matchType = ReconcileCheckMatchType.CONTAINS_ALL_WORDS,
                query = "to, work",
            )

        assertEquals(
            listOf(failingCheck1, failingCheck2),
            ReconcileCheckEvaluator.evaluate(dayGroup, listOf(passingCheck, failingCheck1, failingCheck2)),
        )
    }

    @Test
    fun `evaluate matches entries by entry tag rather than TagGroup tag`() {
        val date = LocalDate.parse("2026-11-25")
        val dayGroup =
            DayGroup(
                date = date,
                tagGroups =
                    listOf(
                        TagGroup(
                            tag = "Mismatch",
                            entries = listOf(entry(date = date, tag = "Feeling", text = "Calm")),
                        ),
                    ),
            )
        val feelingCheck = check(id = "c-feeling", tag = "Feeling")
        val mismatchCheck = check(id = "c-mismatch", tag = "Mismatch")

        assertEquals(
            listOf(mismatchCheck),
            ReconcileCheckEvaluator.evaluate(dayGroup, listOf(feelingCheck, mismatchCheck)),
        )
    }

    @Test
    fun `blank query fails for STARTS_WITH and CONTAINS_ALL_WORDS`() {
        val date = LocalDate.parse("2026-11-25")
        val dayGroup =
            DayGroup(
                date = date,
                tagGroups =
                    listOf(
                        TagGroup(
                            tag = "Activity",
                            entries = listOf(entry(date = date, tag = "Activity", text = "Walked")),
                        ),
                    ),
            )
        val startsWithBlank =
            check(
                id = "c-sw-blank",
                tag = "Activity",
                matchType = ReconcileCheckMatchType.STARTS_WITH,
                query = "   ",
            )
        val containsWordsBlank =
            check(
                id = "c-cw-blank",
                tag = "Activity",
                matchType = ReconcileCheckMatchType.CONTAINS_ALL_WORDS,
                query = " ,  ",
            )

        assertEquals(
            listOf(startsWithBlank, containsWordsBlank),
            ReconcileCheckEvaluator.evaluate(dayGroup, listOf(startsWithBlank, containsWordsBlank)),
        )
    }

    @Test
    fun `CONTAINS_ALL_WORDS splits on non-ASCII whitespace`() {
        val date = LocalDate.parse("2026-11-25")
        val check =
            check(
                id = "c-unicode-ws",
                tag = "Activity",
                matchType = ReconcileCheckMatchType.CONTAINS_ALL_WORDS,
                query = "to\u00A0work",
            )
        val dayGroup =
            DayGroup(
                date = date,
                tagGroups =
                    listOf(
                        TagGroup(
                            tag = "Activity",
                            entries = listOf(entry(date = date, tag = "Activity", text = "Drove to work")),
                        ),
                    ),
            )

        assertEquals(emptyList<ReconcileCheck>(), ReconcileCheckEvaluator.evaluate(dayGroup, listOf(check)))
    }

    private fun check(
        id: String = "check-1",
        tag: String = "Activity",
        daysOfWeek: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        matchType: ReconcileCheckMatchType = ReconcileCheckMatchType.ANY_ENTRY,
        query: String = "",
        enabled: Boolean = true,
    ) = ReconcileCheck(
        id = id,
        tag = tag,
        daysOfWeek = daysOfWeek,
        matchType = matchType,
        query = query,
        enabled = enabled,
    )

    private fun entry(
        date: LocalDate = LocalDate.parse("2026-11-25"),
        tag: String = "Activity",
        text: String = "entry",
    ) = JournalEntry(
        id = "entry-${text.hashCode()}",
        entryTime = LocalDateTime(date.year, date.month, date.day, 9, 0),
        text = text,
        tag = tag,
    )
}
