package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.ramitsuri.notificationjournal.core.data.getColumnIndex
import com.ramitsuri.notificationjournal.core.data.getLongOrNull
import com.ramitsuri.notificationjournal.core.data.getTextOrNull

class MigrationFrom4To5 : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        val entries = getExisting(connection)

        connection.execSQL("DROP TABLE `JournalEntry`")

        connection.execSQL(
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
                ")",
        )

        entries.forEach { entry ->
            connection.prepare(
                "INSERT INTO JournalEntry " +
                    "(id, entry_time, time_zone, text, tag, entry_time_override, uploaded, auto_tagged) " +
                    "VALUES " +
                    " (?, ?, ?, ?, ?, ?, ?, ?)",
            ).use { statement ->
                statement.bindInt(index = 1, value = entry.id)
                statement.bindLong(index = 2, value = entry.entryTimeMillis)
                statement.bindText(index = 3, value = entry.timeZone)
                statement.bindText(index = 4, value = entry.text)
                if (entry.tag == null) {
                    statement.bindNull(index = 5)
                } else {
                    statement.bindText(index = 5, value = entry.tag)
                }
                if (entry.entryTimeOverride == null) {
                    statement.bindNull(index = 6)
                } else {
                    statement.bindLong(index = 6, entry.entryTimeOverride)
                }
                statement.bindBoolean(index = 7, value = false)
                statement.bindBoolean(index = 8, value = false)
                statement.step()
            }
        }
    }

    private fun getExisting(connection: SQLiteConnection): List<JournalEntryV4> {
        val entries = mutableListOf<JournalEntryV4>()
        connection.prepare("SELECT * FROM JournalEntry").use { statement ->
            while (statement.step()) {
                try {
                    val idColumn = statement.getColumnIndex("id")
                    val id = statement.getInt(idColumn)

                    val entryTimeColumn = statement.getColumnIndex("entry_time")
                    val entryTimeMillis = statement.getLong(entryTimeColumn)

                    val zoneIdColumn = statement.getColumnIndex("time_zone")
                    val zoneId = statement.getText(zoneIdColumn)

                    val textColumn = statement.getColumnIndex("text")
                    val text = statement.getText(textColumn)

                    val tagColumn = statement.getColumnIndex("tag")
                    val tag = statement.getTextOrNull(tagColumn)

                    val entryTimeOverrideColumn = statement.getColumnIndex("entry_time_override")
                    val entryTimeOverride = statement.getLongOrNull(entryTimeOverrideColumn)

                    entries.add(
                        JournalEntryV4(
                            id = id,
                            entryTimeMillis = entryTimeMillis,
                            timeZone = zoneId,
                            text = text,
                            tag = tag,
                            entryTimeOverride = entryTimeOverride,
                        ),
                    )
                } catch (e: Exception) {
                    // Do nothing. Continue reading the ones we can
                }
            }
        }
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
