package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.util.TableInfo
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.ramitsuri.notificationjournal.core.data.getColumnIndex
import com.ramitsuri.notificationjournal.core.data.getLongOrNull
import com.ramitsuri.notificationjournal.core.data.getTextOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MigrationFrom4To5Test : BaseMigrationTest() {
    @Test
    fun testMigrateFrom4To5_shouldContainNewColumn() {
        try {
            // Arrange
            createAndGetDataFromV4()

            // Act
            val dbV5 = migrateAndGetDbV5()
            val tableInfo = TableInfo.read(dbV5, "JournalEntry")

            // Assert
            assertTrue(tableInfo.columns.keys.contains("uploaded"))
            assertTrue(tableInfo.columns.keys.contains("auto_tagged"))
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    @Test
    fun testMigrateFrom4To5_shouldMigrateData() {
        try {
            // Arrange
            val v4Entries = createAndGetDataFromV4()

            // Act
            val v5Entries = migrateAndGetDataFromV5()

            // Assert
            v4Entries.forEach { v4Entry ->
                val v5Entry = v5Entries.first { it.id == v4Entry.id }
                assertEquals(v4Entry.entryTime, v5Entry.entryTime)
                assertEquals(v4Entry.timeZone, v5Entry.timeZone)
                assertEquals(v4Entry.text, v5Entry.text)
                assertEquals(v4Entry.tag, v5Entry.tag)
                assertEquals(v4Entry.entryTimeOverride, v5Entry.entryTimeOverride)
                assertEquals(false, v5Entry.uploaded)
                assertEquals(false, v5Entry.autoTagged)
            }
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    private fun migrateAndGetDbV5(): SQLiteConnection {
        return runMigrationAndValidate(5, MigrationFrom4To5())
    }

    private fun migrateAndGetDataFromV5(): List<JournalEntryV5> {
        val data = mutableListOf<JournalEntryV5>()
        val database = migrateAndGetDbV5()
        database.prepare("SELECT * FROM JournalEntry").use { cursor ->
            while (cursor.step()) {
                val id = cursor.getInt(cursor.getColumnIndex("id"))
                val entryTime = cursor.getLong(cursor.getColumnIndex("entry_time"))
                val timeZone = cursor.getText(cursor.getColumnIndex("time_zone"))
                val text = cursor.getText(cursor.getColumnIndex("text"))
                val tag = cursor.getTextOrNull(cursor.getColumnIndex("tag"))
                val entryTimeOverride =
                    cursor.getLongOrNull(cursor.getColumnIndex("entry_time_override"))
                val uploaded = cursor.getBoolean(cursor.getColumnIndex("uploaded"))
                val autoTagged = cursor.getBoolean(cursor.getColumnIndex("auto_tagged"))
                data.add(
                    JournalEntryV5(
                        id = id,
                        entryTime = entryTime,
                        timeZone = timeZone,
                        text = text,
                        tag = tag,
                        entryTimeOverride = entryTimeOverride,
                        uploaded = uploaded,
                        autoTagged = autoTagged,
                    ),
                )
            }
        }
        return data
    }

    private fun createAndGetDataFromV4(): List<JournalEntryV4> {
        val entries =
            listOf(
                JournalEntryV4(
                    id = 1,
                    entryTime = 1706208791347,
                    timeZone = "TZ1",
                    text = "Text 1",
                    tag = null,
                    entryTimeOverride = null,
                ),
                JournalEntryV4(
                    id = 2,
                    entryTime = 1706208791347,
                    timeZone = "TZ2",
                    text = "Text 2",
                    tag = "Tag1",
                    entryTimeOverride = null,
                ),
                JournalEntryV4(
                    id = 3,
                    entryTime = 1706208791347,
                    timeZone = "TZ3",
                    text = "Text 3",
                    tag = null,
                    entryTimeOverride = 1706208791348,
                ),
                JournalEntryV4(
                    id = 4,
                    entryTime = 1706208791347,
                    timeZone = "TZ4",
                    text = "Text 4",
                    tag = "Tag4",
                    entryTimeOverride = 1706208791348,
                ),
            )
        createDatabase(4).apply {
            entries.forEach {
                val tag =
                    if (it.tag == null) {
                        null
                    } else {
                        "'${it.tag}'"
                    }
                execSQL(
                    "INSERT INTO JournalEntry " +
                        "(id,entry_time,time_zone,text,tag,entry_time_override) " +
                        "VALUES(" +
                        "${it.id},${it.entryTime},'${it.timeZone}','${it.text}'," +
                        "$tag,${it.entryTimeOverride}" +
                        ")",
                )
            }
            close()
        }
        return entries
    }

    private data class JournalEntryV4(
        val id: Int,
        val entryTime: Long,
        val timeZone: String,
        val text: String,
        val tag: String?,
        val entryTimeOverride: Long?,
    )

    private data class JournalEntryV5(
        val id: Int,
        val entryTime: Long,
        val timeZone: String,
        val text: String,
        val tag: String?,
        val entryTimeOverride: Long?,
        val uploaded: Boolean,
        val autoTagged: Boolean,
    )
}
