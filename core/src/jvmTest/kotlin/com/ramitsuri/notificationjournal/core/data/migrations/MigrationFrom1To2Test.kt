package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.util.TableInfo
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.use
import com.ramitsuri.notificationjournal.core.data.getColumnIndex
import com.ramitsuri.notificationjournal.core.data.getLongOrNull
import com.ramitsuri.notificationjournal.core.data.getTextOrNull
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MigrationFrom1To2Test : BaseMigrationTest() {
    private val baseEntryTime = Instant.parse("2023-10-10T12:00:00Z").toEpochMilliseconds()
    private val zoneId = "America/New_York"
    private val baseText = "Text"

    @Test
    fun testMigrateFrom1to2_shouldContainNewColumns() {
        try {
            // Arrange
            createDbVersionOne()

            // Act
            val dbv2 = migrateAndGetDbVersionTwo()
            val tableInfo = TableInfo.read(dbv2, "JournalEntry")

            // Assert
            assertTrue(tableInfo.columns.keys.contains("tag"))
            assertTrue(tableInfo.columns.keys.contains("entry_time_override"))
        } catch (ex: Exception) {
            fail(ex.message)
        }
    }

    @Test
    fun testMigrateFrom1to2_shouldContainData() {
        try {
            // Arrange
            createDbVersionOne()

            // Act
            val data = migrateAndGetDataFromV2()

            // Assert
            assertEquals(10, data.size)
            data.sortedBy { it.id }.forEachIndexed { index, journalEntryV2 ->
                assertEquals(index, journalEntryV2.id)
                assertEquals(baseEntryTime + index, journalEntryV2.entryTime)
                assertEquals(zoneId, journalEntryV2.timeZone)
                assertEquals(baseText + index, journalEntryV2.text)
                assertEquals(null, journalEntryV2.tag)
                assertEquals(null, journalEntryV2.entryTimeOverride)
            }
        } catch (ex: Exception) {
            fail(ex.message)
        }
    }

    private fun migrateAndGetDbVersionTwo(): SQLiteConnection {
        return runMigrationAndValidate(2, MigrationFrom1To2())
    }

    private fun migrateAndGetDataFromV2(): List<JournalEntryV2> {
        val data = mutableListOf<JournalEntryV2>()
        val database = migrateAndGetDbVersionTwo()
        database.prepare("SELECT * FROM JournalEntry").use { statement ->
            while (statement.step()) {
                val id = statement.getInt(statement.getColumnIndex("id"))
                val entryTime = statement.getLong(statement.getColumnIndex("entry_time"))
                val timeZone = statement.getText(statement.getColumnIndex("time_zone"))
                val text = statement.getText(statement.getColumnIndex("text"))
                val tag = statement.getTextOrNull(statement.getColumnIndex("tag"))
                val entryTimeOverride =
                    statement.getLongOrNull(statement.getColumnIndex("entry_time_override"))
                data.add(
                    JournalEntryV2(
                        id = id,
                        entryTime = entryTime,
                        timeZone = timeZone,
                        text = text,
                        tag = tag,
                        entryTimeOverride = entryTimeOverride,
                    ),
                )
            }
        }
        database.close()
        return data
    }

    private fun createDbVersionOne() {
        createDatabase(1).apply {
            repeat(10) {
                execSQL(
                    "INSERT INTO JournalEntry (id,entry_time,time_zone,text) " +
                        "VALUES($it,${baseEntryTime + it},'$zoneId','${baseText + it}')",
                )
            }
        }
    }

    private data class JournalEntryV2(
        val id: Int,
        val entryTime: Long,
        val timeZone: String,
        val text: String,
        val tag: String?,
        val entryTimeOverride: Long?,
    )
}
