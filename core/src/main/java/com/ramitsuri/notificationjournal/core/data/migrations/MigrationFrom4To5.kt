package com.ramitsuri.notificationjournal.core.data.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getLongOrNull
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MigrationFrom4To5 : Migration(4, 5) {

    override fun migrate(database: SupportSQLiteDatabase) {
        val entries = getExisting(database)

        database.execSQL("DROP TABLE `JournalEntry`")

        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `JournalEntry` " +
                    "(" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`entry_time` INTEGER NOT NULL, " +
                    "`time_zone` TEXT NOT NULL, " +
                    "`text` TEXT NOT NULL, " +
                    "`tag` TEXT, " +
                    "`entry_time_override` INTEGER, " +
                    "`uploaded` INTEGER NOT NULL DEFAULT 0," +
                    "`auto_tagged` INTEGER NOT NULL DEFAULT 0" +
                    ")"
        )

        val contentValues = ContentValues()
        entries.forEach { entry ->
            contentValues.clear()
            contentValues.put("id", entry.id)
            contentValues.put("entry_time", entry.entryTimeMillis)
            contentValues.put("time_zone", entry.timeZone)
            contentValues.put("text", entry.text)
            contentValues.put("tag", entry.tag)
            contentValues.put("entry_time_override", entry.entryTimeOverride)
            contentValues.put("uploaded", false)
            contentValues.put("auto_tagged", false)
            database.insert("JournalEntry", SQLiteDatabase.CONFLICT_IGNORE, contentValues)
        }
    }

    private fun getExisting(database: SupportSQLiteDatabase): List<JournalEntryV4> {
        val entries = mutableListOf<JournalEntryV4>()
        val cursor = database.query("SELECT * FROM JournalEntry")
        if (!cursor.moveToFirst()) { // Cursor empty
            cursor.close()
            return entries
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

                val tagColumn = cursor.getColumnIndex("tag")
                val tag = cursor.getString(tagColumn)

                val entryTimeOverrideColumn = cursor.getColumnIndex("entry_time_override")
                val entryTimeOverride = cursor.getLongOrNull(entryTimeOverrideColumn)

                entries.add(
                    JournalEntryV4(
                        id = id,
                        entryTimeMillis = entryTimeMillis,
                        timeZone = zoneId,
                        text = text,
                        tag = tag,
                        entryTimeOverride = entryTimeOverride,
                    )
                )
            } catch (e: Exception) {
                // Do nothing. Continue reading the ones we can
            }
        } while (cursor.moveToNext())

        cursor.close()
        return entries
    }

    private data class JournalEntryV4(
        val id: Int,
        val entryTimeMillis: Long,
        val timeZone: String,
        val text: String,
        val tag: String?,
        val entryTimeOverride: Long?,
    )
}
