package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.util.TableInfo
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.use
import com.ramitsuri.notificationjournal.core.data.getColumnIndex
import com.ramitsuri.notificationjournal.core.data.getTextOrNull
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MigrationFrom11To12Test : BaseMigrationTest() {
    @Test
    fun testDictionaryItemIndexCreated() {
        try {
            // Arrange
            val dbV11 = createDatabase(11)
            val tableInfoV11 = TableInfo.read(dbV11, "DictionaryItem")
            assertNull(tableInfoV11.indices?.firstOrNull { it.name == "index_dictionary_word" })

            // Act
            val dbV12 = migrateAndGetDbV12()
            val tableInfoV12 = TableInfo.read(dbV12, "DictionaryItem")

            // Assert
            val index = tableInfoV12.indices!!.first { it.name == "index_dictionary_word" }
            assertNotNull(index)
            assertTrue(index.unique)
            assertTrue(index.columns.contains("word"))
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    @Test
    fun testMigrateFrom11To12_shouldNotHaveTimeZoneColumn() {
        try {
            // Arrange
            createAndGetEntriesFromV11()

            // Act
            val dbV12 = migrateAndGetDbV12()
            val tableInfo = TableInfo.read(dbV12, "JournalEntry")

            // Assert
            assertFalse(tableInfo.columns.containsKey("time_zone"))
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    @Test
    fun testMigrateFrom11To12_shouldMigrateJournalEntryData() {
        try {
            // Arrange
            val v11Data = createAndGetEntriesFromV11()

            // Act
            val connection = migrateAndGetDbV12()
            val v12Data = getEntriesFromV12(connection)

            // Assert
            v11Data.forEach { v11 ->
                val v12 = v12Data.first { it.text == v11.text }
                assertEquals(
                    Instant
                        .fromEpochMilliseconds(v11.entryTime)
                        .toLocalDateTime(TimeZone.of(v11.timeZone))
                        .toString(),
                    v12.entryTime,
                )
                assertEquals(v11.text, v12.text)
                assertEquals(v11.tag, v12.tag)
                assertEquals(v11.uploaded, v12.uploaded)
                assertEquals(v11.autoTagged, v12.autoTagged)
                assertEquals(v11.deleted, v12.deleted)
                assertEquals(v11.reconciled, v12.reconciled)
            }
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    private fun getEntriesFromV12(connection: SQLiteConnection): List<JournalEntryV12> {
        val data = mutableListOf<JournalEntryV12>()
        connection.prepare("SELECT * FROM JournalEntry").use { statement ->
            while (statement.step()) {
                val id = statement.getText(statement.getColumnIndex("id"))
                val entryTime = statement.getText(statement.getColumnIndex("entry_time"))
                val text = statement.getText(statement.getColumnIndex("text"))
                val tag = statement.getTextOrNull(statement.getColumnIndex("tag"))
                val uploaded = statement.getBoolean(statement.getColumnIndex("uploaded"))
                val autoTagged = statement.getBoolean(statement.getColumnIndex("auto_tagged"))
                val deleted = statement.getBoolean(statement.getColumnIndex("deleted"))
                val reconciled = statement.getBoolean(statement.getColumnIndex("reconciled"))
                data.add(
                    JournalEntryV12(
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

    private fun createAndGetEntriesFromV11(): List<JournalEntryV11> {
        val data =
            listOf(
                JournalEntryV11(
                    id = "1",
                    timeZone = "America/New_York",
                    entryTime = 1706208791347,
                    text = "Text 1",
                    tag = null,
                    uploaded = true,
                    autoTagged = true,
                    deleted = false,
                    reconciled = false,
                ),
            )
        createDatabase(11).apply {
            data.forEach {
                val tag =
                    if (it.tag == null) {
                        null
                    } else {
                        "'${it.tag}'"
                    }
                val statement =
                    "INSERT INTO JournalEntry " +
                        "(id,entry_time,time_zone,text,tag,uploaded,auto_tagged,deleted,reconciled) " +
                        "VALUES(" +
                        "'${it.id}',${it.entryTime},'${it.timeZone}','${it.text}'," +
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

    private data class JournalEntryV11(
        val id: String,
        val entryTime: Long,
        val timeZone: String,
        val text: String,
        val tag: String?,
        val uploaded: Boolean,
        val autoTagged: Boolean,
        val deleted: Boolean,
        val reconciled: Boolean,
    )

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

    private fun migrateAndGetDbV12(): SQLiteConnection {
        return runMigrationAndValidate(12, MigrationFrom11To12())
    }
}
