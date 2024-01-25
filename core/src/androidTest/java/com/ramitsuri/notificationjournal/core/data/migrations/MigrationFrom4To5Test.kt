package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.core.database.getLongOrNull
import androidx.room.testing.MigrationTestHelper
import androidx.room.util.TableInfo
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationFrom4To5Test {
    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        listOf(),
        FrameworkSQLiteOpenHelperFactory(),
    )

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
            }
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    private fun migrateAndGetDbV5(): SupportSQLiteDatabase {
        return helper.runMigrationsAndValidate(testDb, 5, true, MigrationFrom4To5())
    }

    private fun migrateAndGetDataFromV5(): List<JournalEntryV5> {
        val database = migrateAndGetDbV5()
        val data = mutableListOf<JournalEntryV5>()
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
                val uploaded = cursor.getInt(cursor.getColumnIndex("uploaded")) == 1
                data.add(
                    JournalEntryV5(
                        id = id,
                        entryTime = entryTime,
                        timeZone = timeZone,
                        text = text,
                        tag = tag,
                        entryTimeOverride = entryTimeOverride,
                        uploaded = uploaded,
                    ),
                )
            } catch (e: Exception) {
                // Do nothing. Continue reading the ones we can
            }
        } while (cursor.moveToNext())
        return data
    }

    private fun createAndGetDataFromV4(): List<JournalEntryV4> {
        val entries = listOf(
            JournalEntryV4(
                id = 1,
                entryTime = 1706208791347,
                timeZone = "TZ1",
                text = "Text 1",
                tag = null,
                entryTimeOverride = null
            ),
            JournalEntryV4(
                id = 2,
                entryTime = 1706208791347,
                timeZone = "TZ2",
                text = "Text 2",
                tag = "Tag1",
                entryTimeOverride = null
            ),
            JournalEntryV4(
                id = 3,
                entryTime = 1706208791347,
                timeZone = "TZ3",
                text = "Text 3",
                tag = null,
                entryTimeOverride = 1706208791348
            ),
            JournalEntryV4(
                id = 4,
                entryTime = 1706208791347,
                timeZone = "TZ4",
                text = "Text 4",
                tag = "Tag4",
                entryTimeOverride = 1706208791348
            )
        )
        helper.createDatabase(testDb, 4).apply {
            entries.forEach {
                val tag = if (it.tag == null) {
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
                            ")"
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
    )
}