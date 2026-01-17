package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.ramitsuri.notificationjournal.core.data.getColumnIndex
import com.ramitsuri.notificationjournal.core.data.getTextOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// Removes TimeZone column, changes entryTime to LocalDateTime from Instant in JournalEntry and EntryConflict
class MigrationFrom11To12 : Migration(11, 12) {
    override fun migrate(connection: SQLiteConnection) {
        migrateEntries(connection)
        migrateConflicts(connection)

        // Missed this from previous migration
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_dictionary_word` ON `DictionaryItem` (`word`)",
        )
    }

    private fun migrateEntries(connection: SQLiteConnection) {
        val entries = getExistingEntries(connection)
        connection.execSQL("DROP TABLE `JournalEntry`")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `JournalEntry` " +
                "(" +
                "`id` TEXT NOT NULL, " +
                "`entry_time` TEXT NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "`tag` TEXT, " +
                "`uploaded` INTEGER NOT NULL DEFAULT 0, " +
                "`auto_tagged` INTEGER NOT NULL DEFAULT 0, " +
                "`deleted` INTEGER NOT NULL DEFAULT 0, " +
                "`reconciled` INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(`id`)" +
                ")",
        )
        entries.forEach { entry ->
            connection.prepare(
                "INSERT INTO JournalEntry " +
                    "(id, entry_time, text, tag, uploaded, auto_tagged, deleted, reconciled) " +
                    "VALUES " +
                    " (?, ?, ?, ?, ?, ?, ?, ?)",
            ).use { statement ->
                statement.bindText(index = 1, value = entry.id)
                statement.bindText(
                    index = 2,
                    value = entry.entryTimeMillis.toLocalDateTimeString(entry.timeZone),
                )
                statement.bindText(index = 3, value = entry.text)
                if (entry.tag == null) {
                    statement.bindNull(index = 4)
                } else {
                    statement.bindText(index = 4, value = entry.tag)
                }
                statement.bindBoolean(index = 5, value = entry.uploaded)
                statement.bindBoolean(index = 6, value = entry.autoTagged)
                statement.bindBoolean(index = 7, value = entry.deleted)
                statement.bindBoolean(index = 8, value = entry.reconciled)
                statement.step()
            }
        }
    }

    private fun getExistingEntries(connection: SQLiteConnection): List<JournalEntryV11> {
        val entries = mutableListOf<JournalEntryV11>()
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

                    val uploadedColumn = statement.getColumnIndex("uploaded")
                    val uploaded = statement.getBoolean(uploadedColumn)

                    val autoTaggedColumn = statement.getColumnIndex("auto_tagged")
                    val autoTagged = statement.getBoolean(autoTaggedColumn)

                    val deletedColumn = statement.getColumnIndex("deleted")
                    val deleted = statement.getBoolean(deletedColumn)

                    val reconciledColumn = statement.getColumnIndex("reconciled")
                    val reconciled = statement.getBoolean(reconciledColumn)

                    entries.add(
                        JournalEntryV11(
                            id = id,
                            entryTimeMillis = entryTimeMillis,
                            timeZone = zoneId,
                            text = text,
                            tag = tag,
                            uploaded = uploaded,
                            autoTagged = autoTagged,
                            deleted = deleted,
                            reconciled = reconciled,
                        ),
                    )
                } catch (e: Exception) {
                    // Do nothing. Continue reading the ones we can
                }
            }
        }
        return entries
    }

    private data class JournalEntryV11(
        val id: String,
        val entryTimeMillis: Long,
        val timeZone: String,
        val text: String,
        val tag: String?,
        val uploaded: Boolean,
        val autoTagged: Boolean,
        val deleted: Boolean,
        val reconciled: Boolean,
    )

    private fun migrateConflicts(connection: SQLiteConnection) {
        val existing = getExistingConflicts(connection)
        connection.execSQL("DROP TABLE `EntryConflict`")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `EntryConflict` " +
                "(" +
                "`id` TEXT NOT NULL, " +
                "`entry_id` TEXT NOT NULL, " +
                "`entry_time` TEXT NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "`tag` TEXT, " +
                "`sender_name` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`)" +
                ")",
        )
        existing.forEach { conflict ->
            connection.prepare(
                "INSERT INTO EntryConflict " +
                    "(id, entry_id, entry_time, text, tag, sender_name) " +
                    "VALUES " +
                    " (?, ?, ?, ?, ?, ?)",
            ).use { statement ->
                statement.bindText(index = 1, value = conflict.id)
                statement.bindText(index = 2, value = conflict.entryId)
                statement.bindText(
                    index = 3,
                    value = conflict.entryTimeMillis.toLocalDateTimeString(),
                )
                statement.bindText(index = 4, value = conflict.text)
                if (conflict.tag == null) {
                    statement.bindNull(index = 5)
                } else {
                    statement.bindText(index = 5, value = conflict.tag)
                }
                statement.bindText(index = 6, value = conflict.senderName)
                statement.step()
            }
        }
    }

    private fun getExistingConflicts(connection: SQLiteConnection): List<EntryConflictV11> {
        val entries = mutableListOf<EntryConflictV11>()
        connection.prepare("SELECT * FROM EntryConflict").use { statement ->
            while (statement.step()) {
                try {
                    val idColumn = statement.getColumnIndex("id")
                    val id = statement.getText(idColumn)

                    val entryIdColumn = statement.getColumnIndex("entry_id")
                    val entryId = statement.getText(entryIdColumn)

                    val entryTimeColumn = statement.getColumnIndex("entry_time")
                    val entryTimeMillis = statement.getLong(entryTimeColumn)

                    val textColumn = statement.getColumnIndex("text")
                    val text = statement.getText(textColumn)

                    val tagColumn = statement.getColumnIndex("tag")
                    val tag = statement.getTextOrNull(tagColumn)

                    val senderNameColumn = statement.getColumnIndex("sender_name")
                    val senderName = statement.getText(senderNameColumn)

                    entries.add(
                        EntryConflictV11(
                            id = id,
                            entryId = entryId,
                            entryTimeMillis = entryTimeMillis,
                            text = text,
                            tag = tag,
                            senderName = senderName,
                        ),
                    )
                } catch (e: Exception) {
                    // Do nothing. Continue reading the ones we can
                }
            }
        }
        return entries
    }

    private data class EntryConflictV11(
        val id: String,
        val entryId: String,
        val entryTimeMillis: Long,
        val text: String,
        val tag: String?,
        val senderName: String,
    )

    @OptIn(ExperimentalTime::class)
    private fun Long.toLocalDateTimeString(timeZone: String? = null) =
        Instant
            .fromEpochMilliseconds(this)
            .toLocalDateTime(timeZone?.let { TimeZone.of(it) } ?: TimeZone.currentSystemDefault())
            .toString()
}
