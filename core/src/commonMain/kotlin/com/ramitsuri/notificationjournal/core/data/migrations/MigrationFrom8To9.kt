package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.use
import com.ramitsuri.notificationjournal.core.data.getColumnIndex
import com.ramitsuri.notificationjournal.core.data.getLongOrNull
import com.ramitsuri.notificationjournal.core.data.getTextOrNull

// Copies "entry_time_override" value to "entry_time" and deletes the column from JournalEntry table
class MigrationFrom8To9 : Migration(8, 9) {

    override fun migrate(connection: SQLiteConnection) {
        val entries = getExisting(connection)
        connection.execSQL("DROP TABLE `JournalEntry`")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `JournalEntry` " +
                    "(" +
                    "`id` TEXT NOT NULL, " +
                    "`entry_time` INTEGER NOT NULL, " +
                    "`time_zone` TEXT NOT NULL, " +
                    "`text` TEXT NOT NULL, " +
                    "`tag` TEXT, " +
                    "`uploaded` INTEGER NOT NULL DEFAULT 0, " +
                    "`auto_tagged` INTEGER NOT NULL DEFAULT 0, " +
                    "`deleted` INTEGER NOT NULL DEFAULT 0, " +
                    "`reconciled` INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(`id`)" +
                    ")"
        )

        entries.forEach { entry ->
            connection.prepare(
                "INSERT INTO JournalEntry " +
                        "(id, entry_time, time_zone, text, tag, uploaded, auto_tagged, deleted, reconciled) " +
                        "VALUES " +
                        " (?, ?, ?, ?, ?, ?, ?, ?, ?)"
            ).use { statement ->
                statement.bindText(index = 1, value = entry.id)
                statement.bindLong(
                    index = 2,
                    value = entry.entryTimeOverrideMillis ?: entry.entryTimeMillis
                )
                statement.bindText(index = 3, value = entry.timeZone)
                statement.bindText(index = 4, value = entry.text)
                if (entry.tag == null) {
                    statement.bindNull(index = 5)
                } else {
                    statement.bindText(index = 5, value = entry.tag)
                }
                statement.bindBoolean(index = 6, value = entry.uploaded)
                statement.bindBoolean(index = 7, value = entry.autoTagged)
                statement.bindBoolean(index = 8, value = entry.deleted)
                statement.bindBoolean(index = 9, value = entry.reconciled)
                statement.step()
            }
        }
    }

    private fun getExisting(connection: SQLiteConnection): List<JournalEntryV8> {
        val entries = mutableListOf<JournalEntryV8>()
        connection.prepare("SELECT * FROM JournalEntry").use { statement ->
            while (statement.step()) {
                try {
                    val idColumn = statement.getColumnIndex("id")
                    val id = statement.getText(idColumn)

                    val entryTimeColumn = statement.getColumnIndex("entry_time")
                    val entryTimeMillis = statement.getLong(entryTimeColumn)

                    val zoneIdColumn = statement.getColumnIndex("time_zone")
                    val zoneId = statement.getText(zoneIdColumn)

                    val textColumn = statement.getColumnIndex("text")
                    val text = statement.getText(textColumn)

                    val tagColumn = statement.getColumnIndex("tag")
                    val tag = statement.getTextOrNull(tagColumn)

                    val entryTimeOverrideColumn = statement.getColumnIndex("entry_time_override")
                    val entryTimeOverrideMillis = statement.getLongOrNull(entryTimeOverrideColumn)

                    val uploadedColumn = statement.getColumnIndex("uploaded")
                    val uploaded = statement.getBoolean(uploadedColumn)

                    val autoTaggedColumn = statement.getColumnIndex("auto_tagged")
                    val autoTagged = statement.getBoolean(autoTaggedColumn)

                    val deletedColumn = statement.getColumnIndex("deleted")
                    val deleted = statement.getBoolean(deletedColumn)

                    val reconciledColumn = statement.getColumnIndex("reconciled")
                    val reconciled = statement.getBoolean(reconciledColumn)

                    entries.add(
                        JournalEntryV8(
                            id = id,
                            entryTimeMillis = entryTimeMillis,
                            timeZone = zoneId,
                            text = text,
                            tag = tag,
                            entryTimeOverrideMillis = entryTimeOverrideMillis,
                            uploaded = uploaded,
                            autoTagged = autoTagged,
                            deleted = deleted,
                            reconciled = reconciled,
                        )
                    )
                } catch (e: Exception) {
                    // Do nothing. Continue reading the ones we can
                }
            }
        }
        return entries
    }

    private data class JournalEntryV8(
        val id: String,
        val entryTimeMillis: Long,
        val timeZone: String,
        val text: String,
        val tag: String?,
        val entryTimeOverrideMillis: Long?,
        val uploaded: Boolean,
        val autoTagged: Boolean,
        val deleted: Boolean,
        val reconciled: Boolean,
    )
}
