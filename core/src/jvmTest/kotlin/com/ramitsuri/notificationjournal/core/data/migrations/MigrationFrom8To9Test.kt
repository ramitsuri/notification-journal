package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.util.TableInfo
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.use
import com.ramitsuri.notificationjournal.core.data.getColumnIndex
import com.ramitsuri.notificationjournal.core.data.getTextOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test

class MigrationFrom8To9Test : BaseMigrationTest() {
    @Test
    fun testMigrateFrom8To9_shouldNotHaveEntryTimeOverrideColumn() {
        try {
            // Arrange
            createAndGetEntriesFromV8()

            // Act
            val dbV9 = migrateAndGetDbV9()
            val tableInfo = TableInfo.read(dbV9, "JournalEntry")

            // Assert
            assertFalse(tableInfo.columns.containsKey("entry_time_override"))
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    @Test
    fun testMigrateFrom8To9_shouldMigrateJournalEntryData() {
        try {
            // Arrange
            val v8Data = createAndGetEntriesFromV8()

            // Act
            val connection = migrateAndGetDbV9()
            val v9Data = getEntriesFromV9(connection)

            // Assert
            v8Data.forEach { v8 ->
                val v9 = v9Data.first { it.text == v8.text }
                if (v8.entryTimeOverride == null) {
                    assertEquals(v8.entryTime, v9.entryTime)
                } else {
                    assertEquals(v8.entryTimeOverride, v9.entryTime)
                }
                assertEquals(v8.timeZone, v9.timeZone)
                assertEquals(v8.text, v9.text)
                assertEquals(v8.tag, v9.tag)
                assertEquals(v8.uploaded, v9.uploaded)
                assertEquals(v8.autoTagged, v9.autoTagged)
                assertEquals(v8.deleted, v9.deleted)
                assertEquals(v8.reconciled, v9.reconciled)
            }
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    private fun getEntriesFromV9(connection: SQLiteConnection): List<JournalEntryV9> {
        val data = mutableListOf<JournalEntryV9>()
        connection.prepare("SELECT * FROM JournalEntry").use { statement ->
            while (statement.step()) {
                val id = statement.getText(statement.getColumnIndex("id"))
                val entryTime = statement.getLong(statement.getColumnIndex("entry_time"))
                val timeZone = statement.getText(statement.getColumnIndex("time_zone"))
                val text = statement.getText(statement.getColumnIndex("text"))
                val tag = statement.getTextOrNull(statement.getColumnIndex("tag"))
                val uploaded = statement.getBoolean(statement.getColumnIndex("uploaded"))
                val autoTagged = statement.getBoolean(statement.getColumnIndex("auto_tagged"))
                val deleted = statement.getBoolean(statement.getColumnIndex("deleted"))
                val reconciled = statement.getBoolean(statement.getColumnIndex("reconciled"))
                data.add(
                    JournalEntryV9(
                        id = id,
                        entryTime = entryTime,
                        timeZone = timeZone,
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

    private fun createAndGetEntriesFromV8(): List<JournalEntryV8> {
        val data =
            listOf(
                JournalEntryV8(
                    id = "1",
                    timeZone = "TZ1",
                    entryTime = 1706208791347,
                    text = "Text 1",
                    tag = null,
                    entryTimeOverride = null,
                    uploaded = true,
                    autoTagged = true,
                    deleted = false,
                    reconciled = false,
                ),
                JournalEntryV8(
                    id = "2",
                    entryTime = 1706208791347,
                    timeZone = "TZ2",
                    text = "Text 2",
                    tag = "Tag1",
                    entryTimeOverride = null,
                    uploaded = true,
                    autoTagged = false,
                    deleted = false,
                    reconciled = false,
                ),
                JournalEntryV8(
                    id = "3",
                    entryTime = 1706208791347,
                    timeZone = "TZ3",
                    text = "Text 3",
                    tag = null,
                    entryTimeOverride = 1706208791348,
                    uploaded = false,
                    autoTagged = true,
                    deleted = false,
                    reconciled = false,
                ),
                JournalEntryV8(
                    id = "4",
                    entryTime = 1706208791347,
                    timeZone = "TZ4",
                    text = "Text 4",
                    tag = "Tag4",
                    entryTimeOverride = 1706208791348,
                    uploaded = true,
                    autoTagged = true,
                    deleted = false,
                    reconciled = true,
                ),
            )
        createDatabase(8).apply {
            data.forEach {
                val tag =
                    if (it.tag == null) {
                        null
                    } else {
                        "'${it.tag}'"
                    }
                val statement =
                    "INSERT INTO JournalEntry " +
                        "(id,entry_time,time_zone,text,tag,entry_time_override,uploaded,auto_tagged,deleted," +
                        "reconciled) " +
                        "VALUES(" +
                        "'${it.id}',${it.entryTime},'${it.timeZone}','${it.text}'," +
                        "$tag,${it.entryTimeOverride},${it.uploaded},${it.autoTagged}," +
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

    private data class JournalEntryV8(
        val id: String,
        val entryTime: Long,
        val timeZone: String,
        val text: String,
        val tag: String?,
        val entryTimeOverride: Long?,
        val uploaded: Boolean,
        val autoTagged: Boolean,
        val deleted: Boolean,
        val reconciled: Boolean,
    )

    private data class JournalEntryV9(
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

    private fun migrateAndGetDbV9(): SQLiteConnection {
        return runMigrationAndValidate(9, MigrationFrom8To9())
    }
}
