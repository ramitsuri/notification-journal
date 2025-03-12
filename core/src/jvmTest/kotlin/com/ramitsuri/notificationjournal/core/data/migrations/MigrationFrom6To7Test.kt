package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.util.TableInfo
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.ramitsuri.notificationjournal.core.data.getColumnIndex
import com.ramitsuri.notificationjournal.core.data.getLongOrNull
import com.ramitsuri.notificationjournal.core.data.getTextOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MigrationFrom6To7Test : BaseMigrationTest() {
    //region JournalEntry

    @Test
    fun testMigrateFrom6To7_shouldChangeJournalEntryIdColumnType() {
        try {
            // Arrange
            createAndGetEntriesFromV6()

            // Act
            val dbV7 = migrateAndGetDbV7()
            val tableInfo = TableInfo.read(dbV7, "JournalEntry")

            // Assert
            assertTrue(tableInfo.columns.containsKey("deleted"))
            assertEquals("INTEGER", tableInfo.columns["deleted"]!!.type)

            assertTrue(tableInfo.columns.containsKey("reconciled"))
            assertEquals("INTEGER", tableInfo.columns["reconciled"]!!.type)
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    @Test
    fun testMigrateFrom6To7_shouldMigrateJournalEntryData() {
        try {
            // Arrange
            val v6Data = createAndGetEntriesFromV6()

            // Act
            val connection = migrateAndGetDbV7()
            val v7Data = getEntriesFromV7(connection)

            // Assert
            v6Data.forEach { v6 ->
                val v7 = v7Data.first { it.text == v6.text }
                assertEquals(v6.entryTime, v7.entryTime)
                assertEquals(v6.timeZone, v7.timeZone)
                assertEquals(v6.text, v7.text)
                assertEquals(v6.tag, v7.tag)
                assertEquals(v6.entryTimeOverride, v7.entryTimeOverride)
                assertEquals(v6.uploaded, v7.uploaded)
                assertEquals(v6.autoTagged, v7.autoTagged)
                assertFalse(v7.deleted)
                assertFalse(v7.reconciled)
            }
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    private fun getEntriesFromV7(connection: SQLiteConnection): List<JournalEntryV7> {
        val data = mutableListOf<JournalEntryV7>()
        connection.prepare("SELECT * FROM JournalEntry").use { statement ->
            while (statement.step()) {
                val id = statement.getText(statement.getColumnIndex("id"))
                val entryTime = statement.getLong(statement.getColumnIndex("entry_time"))
                val timeZone = statement.getText(statement.getColumnIndex("time_zone"))
                val text = statement.getText(statement.getColumnIndex("text"))
                val tag = statement.getTextOrNull(statement.getColumnIndex("tag"))
                val entryTimeOverride =
                    statement.getLongOrNull(statement.getColumnIndex("entry_time_override"))
                val uploaded = statement.getBoolean(statement.getColumnIndex("uploaded"))
                val autoTagged = statement.getBoolean(statement.getColumnIndex("auto_tagged"))
                val deleted = statement.getBoolean(statement.getColumnIndex("deleted"))
                val reconciled = statement.getBoolean(statement.getColumnIndex("reconciled"))
                data.add(
                    JournalEntryV7(
                        id = id,
                        entryTime = entryTime,
                        timeZone = timeZone,
                        text = text,
                        tag = tag,
                        entryTimeOverride = entryTimeOverride,
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

    private fun createAndGetEntriesFromV6(): List<JournalEntryV6> {
        val data =
            listOf(
                JournalEntryV6(
                    id = 1,
                    timeZone = "TZ1",
                    entryTime = 1706208791347,
                    text = "Text 1",
                    tag = null,
                    entryTimeOverride = null,
                    uploaded = true,
                    autoTagged = false,
                ),
                JournalEntryV6(
                    id = 2,
                    entryTime = 1706208791347,
                    timeZone = "TZ2",
                    text = "Text 2",
                    tag = "Tag1",
                    entryTimeOverride = null,
                    uploaded = true,
                    autoTagged = true,
                ),
                JournalEntryV6(
                    id = 3,
                    entryTime = 1706208791347,
                    timeZone = "TZ3",
                    text = "Text 3",
                    tag = null,
                    entryTimeOverride = 1706208791348,
                    uploaded = false,
                    autoTagged = true,
                ),
                JournalEntryV6(
                    id = 4,
                    entryTime = 1706208791347,
                    timeZone = "TZ4",
                    text = "Text 4",
                    tag = "Tag4",
                    entryTimeOverride = 1706208791348,
                    uploaded = false,
                    autoTagged = false,
                ),
            )
        createDatabase(6).apply {
            data.forEach {
                val tag =
                    if (it.tag == null) {
                        null
                    } else {
                        "'${it.tag}'"
                    }
                execSQL(
                    "INSERT INTO JournalEntry " +
                        "(id,entry_time,time_zone,text,tag,entry_time_override,uploaded,auto_tagged) " +
                        "VALUES(" +
                        "${it.id},${it.entryTime},'${it.timeZone}','${it.text}'," +
                        "$tag,${it.entryTimeOverride},${it.uploaded},${it.autoTagged}" +
                        ")",
                )
            }
            close()
        }
        return data
    }

    private data class JournalEntryV6(
        val id: Int,
        val entryTime: Long,
        val timeZone: String,
        val text: String,
        val tag: String?,
        val entryTimeOverride: Long?,
        val uploaded: Boolean,
        val autoTagged: Boolean,
    )

    private data class JournalEntryV7(
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

    //endregion

    private fun migrateAndGetDbV7(): SQLiteConnection {
        return runMigrationAndValidate(7, MigrationFrom6To7())
    }
}
