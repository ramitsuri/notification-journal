package com.ramitsuri.notificationjournal.core.repository

import app.cash.turbine.test
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Entity
import com.ramitsuri.notificationjournal.core.model.sync.Sender
import com.ramitsuri.notificationjournal.core.utils.BaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalRepositoryTest : BaseTest() {
    @Test
    fun sendsEntriesAsExpected() =
        runTest {
            dataSendHelper.sendSuccessful = true
            val entry = journalEntry().copy(id = "1", replacesLocal = true)
            repository.upload(
                listOf(
                    entry,
                ),
            )

            assertEquals(listOf(entry), dataSendHelper.entriesSent)
        }

    @Test
    fun shouldSaveWithReplacesLocalFalseIfSent() =
        runTest {
            dataSendHelper.sendSuccessful = true
            val entry = journalEntry().copy(id = "1", replacesLocal = true)
            db.journalEntryDao().insert(
                listOf(
                    entry,
                ),
            )
            repository.upload(
                listOf(
                    entry,
                ),
            )

            assertFalse(
                db
                    .journalEntryDao()
                    .getAll()
                    .first()
                    .replacesLocal,
            )
        }

    @Test
    fun shouldSaveWithReplacesLocalOriginalIfNotSent() =
        runTest {
            dataSendHelper.sendSuccessful = false
            val entries =
                listOf(
                    journalEntry().copy(id = "1", replacesLocal = true),
                    journalEntry().copy(id = "2", replacesLocal = false),
                )
            db.journalEntryDao().insert(entries)
            repository.upload(entries)

            val entriesFromDb = db.journalEntryDao().getAll()
            assertTrue(entriesFromDb.first { it.id == "1" }.replacesLocal)
            assertFalse(entriesFromDb.first { it.id == "2" }.replacesLocal)
        }

    @Test
    fun shouldReplaceExistingIfReplacesLocalTrue() =
        runTest {
            db.journalEntryDao().insert(
                listOf(
                    journalEntry().copy(id = "1", text = "one"),
                ),
            )
            repository.handlePayload(
                Entity.Entries(
                    sender = Sender(name = "name", id = "id"),
                    data =
                        listOf(
                            journalEntry().copy(id = "1", text = "one-corrected", replacesLocal = true),
                        ),
                ),
            )
            val entries = db.journalEntryDao().getAll()
            assertEquals(1, entries.size)
            assertEquals("one-corrected", entries.first().text)
        }

    @Test
    fun shouldCreateConflictIfReplacesLocalFalseAndContentDifferent() =
        runTest {
            db.journalEntryDao().insert(
                listOf(
                    journalEntry().copy(id = "1", text = "one"),
                ),
            )
            repository.handlePayload(
                Entity.Entries(
                    sender = Sender(name = "name", id = "id"),
                    data =
                        listOf(
                            journalEntry().copy(id = "1", text = "one-corrected", replacesLocal = false),
                        ),
                ),
            )
            val entries = db.journalEntryDao().getAll()
            assertEquals(1, entries.size)
            assertEquals("one", entries.first().text)

            val conflicts = db.entryConflictDao().getFlow().first()
            assertEquals(1, conflicts.size)
            assertEquals("one-corrected", conflicts.first().text)
            assertEquals("1", conflicts.first().entryId)
        }

    @Test
    fun shouldNotCreateConflictIfReplacesLocalFalseAndContentSame() =
        runTest {
            db.journalEntryDao().insert(
                listOf(
                    journalEntry().copy(id = "1", text = "one"),
                ),
            )
            repository.handlePayload(
                Entity.Entries(
                    sender = Sender(name = "name", id = "id"),
                    data =
                        listOf(
                            journalEntry().copy(id = "1", text = "one", replacesLocal = false),
                        ),
                ),
            )
            val entries = db.journalEntryDao().getAll()
            assertEquals(1, entries.size)
            assertEquals("one", entries.first().text)

            db.entryConflictDao().getFlow().test { expectNoEvents() }
        }

    @Test
    fun shouldCreateConflictIfReplacesLocalFalseAndContentDifferentAndUntagged() =
        runTest {
            db.journalEntryDao().insert(
                listOf(
                    journalEntry().copy(id = "1", text = "one"),
                ),
            )
            repository.handlePayload(
                Entity.Entries(
                    sender = Sender(name = "name", id = "id"),
                    data =
                        listOf(
                            journalEntry().copy(id = "1", text = "one-", replacesLocal = false),
                        ),
                ),
            )
            val entries = db.journalEntryDao().getAll()
            assertEquals(1, entries.size)
            assertEquals("one", entries.first().text)

            db.entryConflictDao().getFlow().test {
                val conflicts = awaitItem()
                assertEquals(1, conflicts.size)
                assertEquals("one-", conflicts.first().text)
                assertEquals("1", conflicts.first().entryId)
            }
        }

    @Test
    fun getSuggestions_matchesAllWordsRegardlessOfOrder() =
        runTest {
            val tag = "Health"
            val entries =
                listOf(
                    journalEntry().copy(id = "1", text = "Morning coffee at 7am", tag = tag),
                    journalEntry().copy(id = "2", text = "Evening coffee decaf", tag = tag),
                    journalEntry().copy(id = "3", text = "Morning walk in the park", tag = tag),
                    journalEntry().copy(id = "4", text = "Took vitamins in the morning with coffee", tag = tag),
                    journalEntry().copy(id = "5", text = "Coffee and morning walk", tag = "Personal"),
                )
            db.journalEntryDao().insert(entries)

            // Exact order "morning coffee"
            val inOrder = repository.getSuggestions("morning coffee", tag)
            assertEquals(listOf("Morning coffee at 7am", "Took vitamins in the morning with coffee"), inOrder)

            // Reverse order "coffee morning"
            val reverseOrder = repository.getSuggestions("coffee morning", tag)
            assertEquals(listOf("Morning coffee at 7am", "Took vitamins in the morning with coffee"), reverseOrder)

            // Multiple spaces between words
            val multipleSpaces = repository.getSuggestions("coffee   morning", tag)
            assertEquals(listOf("Morning coffee at 7am", "Took vitamins in the morning with coffee"), multipleSpaces)

            // Single word
            val singleWord = repository.getSuggestions("decaf", tag)
            assertEquals(listOf("Evening coffee decaf"), singleWord)

            // Exact entry text typed is excluded
            val exactMatch = repository.getSuggestions("Evening coffee decaf", tag)
            assertTrue(exactMatch.isEmpty())

            // No match
            val noMatch = repository.getSuggestions("tea morning", tag)
            assertTrue(noMatch.isEmpty())

            // Tag with no entries or NO_TAG
            val noTag = repository.getSuggestions("coffee", Tag.NO_TAG.value)
            assertTrue(noTag.isEmpty())
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
