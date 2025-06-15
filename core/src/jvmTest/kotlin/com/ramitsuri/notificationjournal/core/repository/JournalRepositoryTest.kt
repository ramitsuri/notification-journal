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

            assertFalse(db.journalEntryDao().getAll().first().replacesLocal)
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
