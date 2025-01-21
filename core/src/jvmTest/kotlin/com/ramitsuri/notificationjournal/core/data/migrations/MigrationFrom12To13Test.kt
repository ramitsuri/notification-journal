package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.use
import com.ramitsuri.notificationjournal.core.data.getColumnIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MigrationFrom12To13Test : BaseMigrationTest() {
    @Test
    fun testMigrateFrom12To13_shouldMigrateJournalEntryNoTags() {
        try {
            // Arrange
            val v12Data = createAndGetEntriesFromV12()

            // Act
            val connection = migrateAndGetDbV13()
            val v13Data = getEntriesFromV13(connection)

            // Assert
            v12Data.forEach { v12 ->
                val v13 = v13Data.first { it.id == v12.id }
                assertEquals(v12.entryTime, v13.entryTime)
                assertEquals(v12.text, v13.text)

                assertNotNull(v13.tag)
                assertNotEquals("null", v13.tag)
                assertTrue(v13.tag.isNotEmpty())
                assertTrue(v13.tag.isNotBlank())

                assertEquals(v12.uploaded, v13.uploaded)
                assertEquals(v12.autoTagged, v13.autoTagged)
                assertEquals(v12.deleted, v13.deleted)
                assertEquals(v12.reconciled, v13.reconciled)
            }
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    private fun getEntriesFromV13(connection: SQLiteConnection): List<JournalEntryV13> {
        val data = mutableListOf<JournalEntryV13>()
        connection.prepare("SELECT * FROM JournalEntry").use { statement ->
            while (statement.step()) {
                val id = statement.getText(statement.getColumnIndex("id"))
                val entryTime = statement.getText(statement.getColumnIndex("entry_time"))
                val text = statement.getText(statement.getColumnIndex("text"))
                val tag = statement.getText(statement.getColumnIndex("tag"))
                val uploaded = statement.getBoolean(statement.getColumnIndex("uploaded"))
                val autoTagged = statement.getBoolean(statement.getColumnIndex("auto_tagged"))
                val deleted = statement.getBoolean(statement.getColumnIndex("deleted"))
                val reconciled = statement.getBoolean(statement.getColumnIndex("reconciled"))
                data.add(
                    JournalEntryV13(
                        id = id,
                        entryTime = entryTime,
                        text = text,
                        tag = tag,
                        uploaded = uploaded,
                        autoTagged = autoTagged,
                        deleted = deleted,
                        reconciled = reconciled,
                    ),
                )
            }
        }
        return data
    }

    private fun createAndGetEntriesFromV12(): List<JournalEntryV12> {
        val data =
            listOf(
                JournalEntryV12(
                    id = "1",
                    entryTime = "2024-01-01T00:00:00",
                    text = "Text 1",
                    tag = null,
                    uploaded = true,
                    autoTagged = true,
                    deleted = false,
                    reconciled = false,
                ),
                JournalEntryV12(
                    id = "2",
                    entryTime = "2024-01-01T00:00:00",
                    text = "Text 1",
                    tag = "",
                    uploaded = true,
                    autoTagged = true,
                    deleted = false,
                    reconciled = false,
                ),
                JournalEntryV12(
                    id = "3",
                    entryTime = "2024-01-01T00:00:00",
                    text = "Text 1",
                    tag = "null",
                    uploaded = true,
                    autoTagged = true,
                    deleted = false,
                    reconciled = false,
                ),
                JournalEntryV12(
                    id = "4",
                    entryTime = "2024-01-01T00:00:00",
                    text = "Text 1",
                    tag = "   ",
                    uploaded = true,
                    autoTagged = true,
                    deleted = false,
                    reconciled = false,
                ),
                JournalEntryV12(
                    id = "5",
                    entryTime = "2024-01-01T00:00:00",
                    text = "Text 1",
                    tag = "Tag",
                    uploaded = true,
                    autoTagged = true,
                    deleted = false,
                    reconciled = false,
                ),
            )
        createDatabase(12).apply {
            data.forEach {
                val tag =
                    if (it.tag == null) {
                        null
                    } else {
                        "'${it.tag}'"
                    }
                val statement =
                    "INSERT INTO JournalEntry " +
                        "(id,entry_time,text,tag,uploaded,auto_tagged,deleted,reconciled) " +
                        "VALUES(" +
                        "'${it.id}','${it.entryTime}','${it.text}'," +
                        "$tag,${it.uploaded},${it.autoTagged}," +
                        "${it.deleted},${it.reconciled}" +
                        ")"
                execSQL(
                    statement,
                )
            }
            close()
        }
        return data
    }

    private data class JournalEntryV12(
        val id: String,
        val entryTime: String,
        val text: String,
        val tag: String?,
        val uploaded: Boolean,
        val autoTagged: Boolean,
        val deleted: Boolean,
        val reconciled: Boolean,
    )

    private data class JournalEntryV13(
        val id: String,
        val entryTime: String,
        val text: String,
        val tag: String,
        val uploaded: Boolean,
        val autoTagged: Boolean,
        val deleted: Boolean,
        val reconciled: Boolean,
    )

    private fun migrateAndGetDbV13(): SQLiteConnection {
        return runMigrationAndValidate(13, MigrationFrom12To13())
    }

    @Test
    fun testMigrateFrom12To13_shouldMigrateConflictNoTags() {
        try {
            // Arrange
            val v12Data = createAndGetConflictsFromV12()

            // Act
            val connection = migrateAndGetDbV13()
            val v13Data = getConflictsFromV13(connection)

            // Assert
            v12Data.forEach { v12 ->
                val v13 = v13Data.first { it.id == v12.id }
                assertEquals(v12.entryTime, v13.entryTime)
                assertEquals(v12.text, v13.text)

                assertNotNull(v13.tag)
                assertNotEquals("null", v13.tag)
                assertTrue(v13.tag.isNotEmpty())
                assertTrue(v13.tag.isNotBlank())

                assertEquals(v12.senderName, v13.senderName)
            }
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    private fun getConflictsFromV13(connection: SQLiteConnection): List<ConflictV13> {
        val data = mutableListOf<ConflictV13>()
        connection.prepare("SELECT * FROM EntryConflict").use { statement ->
            while (statement.step()) {
                val id = statement.getText(statement.getColumnIndex("id"))
                val entryId = statement.getText(statement.getColumnIndex("entry_id"))
                val entryTime = statement.getText(statement.getColumnIndex("entry_time"))
                val text = statement.getText(statement.getColumnIndex("text"))
                val tag = statement.getText(statement.getColumnIndex("tag"))
                val senderName = statement.getText(statement.getColumnIndex("sender_name"))
                data.add(
                    ConflictV13(
                        id = id,
                        entryId = entryId,
                        entryTime = entryTime,
                        text = text,
                        tag = tag,
                        senderName = senderName,
                    ),
                )
            }
        }
        return data
    }

    private fun createAndGetConflictsFromV12(): List<ConflictV12> {
        val data =
            listOf(
                ConflictV12(
                    id = "1",
                    entryId = "1",
                    entryTime = "2024-01-01T00:00:00",
                    text = "Text 1",
                    tag = null,
                    senderName = "sender",
                ),
                ConflictV12(
                    id = "2",
                    entryId = "2",
                    entryTime = "2024-01-01T00:00:00",
                    text = "Text 1",
                    tag = "",
                    senderName = "sender",
                ),
                ConflictV12(
                    id = "3",
                    entryId = "3",
                    entryTime = "2024-01-01T00:00:00",
                    text = "Text 1",
                    tag = "null",
                    senderName = "sender",
                ),
                ConflictV12(
                    id = "4",
                    entryId = "4",
                    entryTime = "2024-01-01T00:00:00",
                    text = "Text 1",
                    tag = "   ",
                    senderName = "sender",
                ),
                ConflictV12(
                    id = "5",
                    entryId = "5",
                    entryTime = "2024-01-01T00:00:00",
                    text = "Text 1",
                    tag = "Tag",
                    senderName = "sender",
                ),
            )
        createDatabase(12).apply {
            data.forEach {
                val tag =
                    if (it.tag == null) {
                        null
                    } else {
                        "'${it.tag}'"
                    }
                val statement =
                    "INSERT INTO EntryConflict " +
                        "(id, entry_id, entry_time, text, tag, sender_name) " +
                        "VALUES(" +
                        "'${it.id}','${it.entryId}','${it.entryTime}','${it.text}'," +
                        "$tag,'${it.senderName}'" +
                        ")"
                execSQL(
                    statement,
                )
            }
            close()
        }
        return data
    }

    private data class ConflictV12(
        val id: String,
        val entryId: String,
        val entryTime: String,
        val text: String,
        val tag: String?,
        val senderName: String,
    )

    private data class ConflictV13(
        val id: String,
        val entryId: String,
        val entryTime: String,
        val text: String,
        val tag: String,
        val senderName: String,
    )
}
