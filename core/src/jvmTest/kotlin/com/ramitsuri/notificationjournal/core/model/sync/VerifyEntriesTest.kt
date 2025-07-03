package com.ramitsuri.notificationjournal.core.model.sync

import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VerifyEntriesTest {
    @Test
    fun testUnmatchedEntriesIsEmptyIfEntriesMatch() {
        val entries =
            VerifyEntries.Verification(
                entries =
                    listOf(
                        journalEntry().copy(id = "1", text = "one"),
                    ),
            )

        val otherEntries =
            VerifyEntries.Verification(
                entries =
                    listOf(
                        journalEntry().copy(id = "1", text = "one"),
                    ),
            )

        assertTrue(entries.unmatchedEntries(otherEntries).isEmpty())
    }

    @Test
    fun testUnmatchedEntriesIsNotEmptyIfEntriesDontMatch() {
        val entries =
            VerifyEntries.Verification(
                entries =
                    listOf(
                        journalEntry().copy(id = "1", text = "one"),
                    ),
            )

        val otherEntries =
            VerifyEntries.Verification(
                entries =
                    listOf(
                        journalEntry().copy(id = "2", text = "two"),
                    ),
            )

        assertFalse(entries.unmatchedEntries(otherEntries).isEmpty())
    }

    @Test
    fun testUnmatchedEntriesIsNotEmptyIfMoreOtherEntries() {
        val entries =
            VerifyEntries.Verification(
                entries =
                    listOf(
                        journalEntry().copy(id = "1", text = "one"),
                    ),
            )

        val otherEntries =
            VerifyEntries.Verification(
                entries =
                    listOf(
                        journalEntry().copy(id = "1", text = "one"),
                        journalEntry().copy(id = "2", text = "two"),
                    ),
            )

        assertFalse(entries.unmatchedEntries(otherEntries).isEmpty())
    }

    @Test
    fun testUnmatchedEntriesIsNotEmptyIfMoreEntries() {
        val entries =
            VerifyEntries.Verification(
                entries =
                    listOf(
                        journalEntry().copy(id = "1", text = "one"),
                        journalEntry().copy(id = "2", text = "two"),
                    ),
            )

        val otherEntries =
            VerifyEntries.Verification(
                entries =
                    listOf(
                        journalEntry().copy(id = "1", text = "one"),
                    ),
            )

        assertFalse(entries.unmatchedEntries(otherEntries).isEmpty())
    }

    @Test
    fun testUnmatchedEntriesIsNotEmptyIfMoreOtherEntriesWithEmptyEntries() {
        val entries =
            VerifyEntries.Verification(
                entries = listOf(),
            )

        val otherEntries =
            VerifyEntries.Verification(
                entries =
                    listOf(
                        journalEntry().copy(id = "1", text = "one"),
                    ),
            )

        assertFalse(entries.unmatchedEntries(otherEntries).isEmpty())
    }

    @Test
    fun testUnmatchedEntriesIsNotEmptyIfMoreEntriesWithEmptyOtherEntries() {
        val entries =
            VerifyEntries.Verification(
                entries =
                    listOf(
                        journalEntry().copy(id = "1", text = "one"),
                    ),
            )

        val otherEntries =
            VerifyEntries.Verification(
                entries = listOf(),
            )

        assertFalse(entries.unmatchedEntries(otherEntries).isEmpty())
    }

    private fun journalEntry() =
        JournalEntry(
            id = "id",
            entryTime = LocalDateTime.parse("2024-12-25T12:00:00"),
            text = "Test",
            tag = Tag.NO_TAG.value,
            uploaded = false,
            replacesLocal = false,
            deleted = false,
            reconciled = false,
        )
}
