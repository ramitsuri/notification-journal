package com.ramitsuri.notificationjournal.core.repository

import app.cash.turbine.test
import com.ramitsuri.notificationjournal.core.model.ForceUploadAllStatus
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

    @Test
    fun upload_abortsRemainingChunksWhenSendFails() =
        runTest {
            dataSendHelper.sendSuccessful = false
            val entries =
                (1..25).map { index ->
                    journalEntry().copy(id = "$index", text = "Entry $index")
                }
            repository.upload(entries)

            assertEquals(1, dataSendHelper.sendEntriesCallCount)
        }

    @Test
    fun forceUploadAll_abortsRemainingDaysWhenSendFails() =
        runTest {
            val entries =
                listOf(
                    journalEntry().copy(id = "1", entryTime = LocalDateTime.parse("2024-12-01T10:00:00")),
                    journalEntry().copy(id = "2", entryTime = LocalDateTime.parse("2024-12-02T10:00:00")),
                    journalEntry().copy(id = "3", entryTime = LocalDateTime.parse("2024-12-03T10:00:00")),
                )
            db.journalEntryDao().insert(entries)

            dataSendHelper.sendSuccessful = false

            repository.forceUploadAll().test {
                assertEquals(ForceUploadAllStatus.Initial, awaitItem())
                val uploading = awaitItem() as ForceUploadAllStatus.Uploading
                assertEquals(0, uploading.uploadedDaysCount)
                assertEquals(3, uploading.failedDaysCount)
                assertEquals(3, uploading.totalToUploadDaysCount)

                val done = awaitItem() as ForceUploadAllStatus.Done
                assertEquals(0, done.uploadedDaysCount)
                assertEquals(3, done.failedDaysCount)
                assertEquals(3, done.totalToUploadDaysCount)
                awaitComplete()
            }

            assertEquals(1, dataSendHelper.clearDaysAndInsertEntriesCalls.size)
        }

    @Test
    fun deleteDeleted_onlyDeletesUploadedDeletedEntries() =
        runTest {
            val entry1 = journalEntry().copy(id = "1", deleted = true, uploaded = true)
            val entry2 = journalEntry().copy(id = "2", deleted = true, uploaded = false)
            val entry3 = journalEntry().copy(id = "3", deleted = false, uploaded = true)
            val entry4 = journalEntry().copy(id = "4", deleted = false, uploaded = false)

            db.journalEntryDao().insert(listOf(entry1, entry2, entry3, entry4))

            db.journalEntryDao().deleteDeleted()

            org.junit.Assert.assertNull(db.journalEntryDao().get("1"))
            org.junit.Assert.assertNotNull(db.journalEntryDao().get("2"))
            org.junit.Assert.assertNotNull(db.journalEntryDao().get("3"))
            org.junit.Assert.assertNotNull(db.journalEntryDao().get("4"))
        }

    @Test
    fun handlePayload_conflictIdempotency() =
        runTest {
            db.journalEntryDao().insert(
                listOf(
                    journalEntry().copy(id = "1", text = "original"),
                ),
            )
            val conflictPayload =
                Entity.Entries(
                    sender = Sender(name = "name", id = "id"),
                    data =
                        listOf(
                            journalEntry().copy(id = "1", text = "conflicting", replacesLocal = false),
                        ),
                )

            // Deliver first time
            repository.handlePayload(conflictPayload)
            val conflictsFirst = db.entryConflictDao().getFlow().first()
            assertEquals(1, conflictsFirst.size)

            // Deliver second time (re-delivery)
            repository.handlePayload(conflictPayload)
            val conflictsSecond = db.entryConflictDao().getFlow().first()
            assertEquals(1, conflictsSecond.size)
            assertEquals("conflicting", conflictsSecond.first().text)
        }

    @Test
    fun handleWorkPayload_savesEntriesWithUploadedFalseAndReplacesLocalFalse() =
        runTest {
            val payload =
                workPayload(
                    journalEntry().copy(
                        id = "work-1",
                        text = "Work entry 1",
                        uploaded = true,
                        replacesLocal = true,
                    ),
                    journalEntry().copy(
                        id = "work-2",
                        text = "Work entry 2",
                        uploaded = true,
                        replacesLocal = false,
                    ),
                )

            repository.handleWorkPayload(payload)

            val entry1 = repository.get("work-1")
            val entry2 = repository.get("work-2")

            org.junit.Assert.assertNotNull(entry1)
            org.junit.Assert.assertNotNull(entry2)
            assertFalse(entry1!!.uploaded)
            assertFalse(entry1.replacesLocal)
            assertEquals("Work entry 1", entry1.text)
            assertFalse(entry2!!.uploaded)
            assertFalse(entry2.replacesLocal)
            assertEquals("Work entry 2", entry2.text)
        }

    @Test
    fun handleWorkPayload_entriesReturnedByGetForUploadAndScheduledForSync() =
        runTest {
            val payload =
                workPayload(
                    journalEntry().copy(id = "work-1", text = "Work note"),
                )

            repository.handleWorkPayload(payload)

            val forUpload = repository.getForUpload()
            assertEquals(1, forUpload.size)
            assertEquals("work-1", forUpload.first().id)
            assertFalse(forUpload.first().uploaded)

            dataSendHelper.sendSuccessful = true
            repository.uploadAll()

            assertEquals(listOf("work-1"), dataSendHelper.entriesSent.map { it.id })
            assertTrue(repository.get("work-1")!!.uploaded)
        }

    private fun workPayload(vararg entries: JournalEntry) =
        Entity.Entries(
            sender = Sender(name = "Work Mac", id = "work-mac"),
            data = entries.toList(),
        )

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
