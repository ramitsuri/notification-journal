package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.ramitsuri.notificationjournal.core.data.getColumnIndex

class MigrationFrom1To2 : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        val journalEntriesV1 = getExisting(connection)

        connection.execSQL("DROP TABLE `JournalEntry`")

        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `JournalEntry` " +
                "(" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`entry_time` INTEGER NOT NULL, " +
                "`time_zone` TEXT NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "`tag` TEXT, " +
                "`entry_time_override` INTEGER" +
                ")",
        )
        journalEntriesV1.forEach { journalEntryV1 ->
            connection.prepare(
                "INSERT INTO JournalEntry " +
                    "(id, entry_time, time_zone, text, tag, entry_time_override) " +
                    "VALUES " +
                    "(?, ?, ?, ?, ?, ?)",
            ).use { statement ->
                statement.bindInt(index = 1, value = journalEntryV1.id)
                statement.bindLong(index = 2, value = journalEntryV1.entryTimeMillis)
                statement.bindText(index = 3, value = journalEntryV1.timeZone)
                statement.bindText(index = 4, value = journalEntryV1.text)
                statement.bindNull(index = 5)
                statement.bindNull(index = 6)
                statement.step()
            }
        }
    }

    private fun getExisting(connection: SQLiteConnection): List<JournalEntryV1> {
        val journalEntriesV1 = mutableListOf<JournalEntryV1>()
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

                    journalEntriesV1.add(
                        JournalEntryV1(
                            id = id,
                            entryTimeMillis = entryTimeMillis,
                            timeZone = zoneId,
                            text = text,
                        ),
                    )
                } catch (e: Exception) {
                    // Do nothing. Continue reading the ones we can
                }
            }
        }
        return journalEntriesV1
    }

    private data class JournalEntryV1(
        val id: Int,
        val entryTimeMillis: Long,
        val timeZone: String,
        val text: String,
    )
}
