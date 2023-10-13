package com.ramitsuri.notificationjournal.core.data.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MigrationFrom1To2 : Migration(1, 2) {

    override fun migrate(database: SupportSQLiteDatabase) {
        val journalEntriesV1 = getExisting(database)

        database.execSQL("DROP TABLE `JournalEntry`")

        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `JournalEntry` " +
                    "(" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`entry_time` INTEGER NOT NULL, " +
                    "`time_zone` TEXT NOT NULL, " +
                    "`text` TEXT NOT NULL, " +
                    "`tag` TEXT, " +
                    "`entry_time_override` INTEGER" +
                    ")"
        )
        val tagValue: String? = null
        val entryTimeOverride: Long? = null
        val contentValues = ContentValues()
        journalEntriesV1.forEach { journalEntryV1 ->
            contentValues.clear()
            contentValues.put("id", journalEntryV1.id)
            contentValues.put("entry_time", journalEntryV1.entryTimeMillis)
            contentValues.put("time_zone", journalEntryV1.timeZone)
            contentValues.put("text", journalEntryV1.text)
            contentValues.put("tag", tagValue)
            contentValues.put("entry_time_override", entryTimeOverride)
            database.insert("JournalEntry", SQLiteDatabase.CONFLICT_IGNORE, contentValues)
        }
    }

    private fun getExisting(database: SupportSQLiteDatabase): List<JournalEntryV1> {
        val journalEntriesV1 = mutableListOf<JournalEntryV1>()
        val cursor = database.query("SELECT * FROM JournalEntry")
        if (!cursor.moveToFirst()) { // Cursor empty
            cursor.close()
            return journalEntriesV1
        }
        do {
            try {
                val idColumn = cursor.getColumnIndex("id")
                val id = cursor.getInt(idColumn)

                val entryTimeColumn = cursor.getColumnIndex("entry_time")
                val entryTimeMillis = cursor.getLong(entryTimeColumn)

                val zoneIdColumn = cursor.getColumnIndex("time_zone")
                val zoneId = cursor.getString(zoneIdColumn)

                val textColumn = cursor.getColumnIndex("text")
                val text = cursor.getString(textColumn)

                journalEntriesV1.add(
                    JournalEntryV1(
                        id = id,
                        entryTimeMillis = entryTimeMillis,
                        timeZone = zoneId,
                        text = text
                    )
                )
            } catch (e: Exception) {
                // Do nothing. Continue reading the ones we can
            }
        } while (cursor.moveToNext())

        cursor.close()
        return journalEntriesV1
    }

    private data class JournalEntryV1(
        val id: Int,
        val entryTimeMillis: Long,
        val timeZone: String,
        val text: String,
    )
}
