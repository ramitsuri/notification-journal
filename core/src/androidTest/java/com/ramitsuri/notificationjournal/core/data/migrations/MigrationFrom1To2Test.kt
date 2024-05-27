package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.core.database.getLongOrNull
import androidx.room.testing.MigrationTestHelper
import androidx.room.util.TableInfo
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationFrom1To2Test {
    private val testDb = "migration-test"
    private val baseEntryTime = Instant.parse("2023-10-10T12:00:00Z").toEpochMilliseconds()
    private val zoneId = "America/New_York"
    private val baseText = "Text"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        listOf(),
        FrameworkSQLiteOpenHelperFactory(),
    )

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

    private fun migrateAndGetDbVersionTwo(): SupportSQLiteDatabase {
        return helper.runMigrationsAndValidate(testDb, 2, true, MigrationFrom1To2())
    }

    private fun migrateAndGetDataFromV2(): List<JournalEntryV2> {
        val database = migrateAndGetDbVersionTwo()
        val data = mutableListOf<JournalEntryV2>()
        val cursor = database.query("SELECT * FROM JournalEntry")
        if (!cursor.moveToFirst()) { // Cursor empty
            cursor.close()
            return data
        }
        do {
            try {
                val id = cursor.getInt(cursor.getColumnIndex("id"))
                val entryTime = cursor.getLong(cursor.getColumnIndex("entry_time"))
                val timeZone = cursor.getString(cursor.getColumnIndex("time_zone"))
                val text = cursor.getString(cursor.getColumnIndex("text"))
                val tag = cursor.getString(cursor.getColumnIndex("tag"))
                val entryTimeOverride =
                    cursor.getLongOrNull(cursor.getColumnIndex("entry_time_override"))
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
            } catch (e: Exception) {
                // Do nothing. Continue reading the ones we can
            }
        } while (cursor.moveToNext())
        return data
    }

    private fun createDbVersionOne() {
        helper.createDatabase(testDb, 1).apply {
            repeat(10) {
                execSQL(
                    "INSERT INTO JournalEntry (id,entry_time,time_zone,text) " +
                            "VALUES($it,${baseEntryTime + it},'$zoneId','${baseText + it}')",
                )
            }
            close()
        }
    }

    private data class JournalEntryV2(
        val id: Int,
        val entryTime: Long,
        val timeZone: String,
        val text: String,
        val tag: String?,
        val entryTimeOverride: Long?
    )
}