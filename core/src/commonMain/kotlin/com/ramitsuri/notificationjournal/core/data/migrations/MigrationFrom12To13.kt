package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.use
import com.ramitsuri.notificationjournal.core.data.getColumnIndex
import com.ramitsuri.notificationjournal.core.model.Tag

// Makes Tag column non nullable in JournalEntry and EntryConflict
class MigrationFrom12To13 : Migration(12, 13) {
    override fun migrate(connection: SQLiteConnection) {
        migrateEntries(connection)
        migrateConflicts(connection)
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
                "`tag` TEXT NOT NULL, " +
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
                statement.bindText(index = 2, value = entry.entryTime)
                statement.bindText(index = 3, value = entry.text)
                statement.bindText(index = 4, value = entry.tag.getTag())
                statement.bindBoolean(index = 5, value = entry.uploaded)
                statement.bindBoolean(index = 6, value = entry.autoTagged)
                statement.bindBoolean(index = 7, value = entry.deleted)
                statement.bindBoolean(index = 8, value = entry.reconciled)
                statement.step()
            }
        }
    }

    private fun getExistingEntries(connection: SQLiteConnection): List<JournalEntryV12> {
        val entries = mutableListOf<JournalEntryV12>()
        connection.prepare("SELECT * FROM JournalEntry").use { statement ->
            while (statement.step()) {
                try {
                    val idColumn = statement.getColumnIndex("id")
                    val id = statement.getText(idColumn)

                    val entryTimeColumn = statement.getColumnIndex("entry_time")
                    val entryTime = statement.getText(entryTimeColumn)

                    val textColumn = statement.getColumnIndex("text")
                    val text = statement.getText(textColumn)

                    val tagColumn = statement.getColumnIndex("tag")
                    val tag = statement.getText(tagColumn)

                    val uploadedColumn = statement.getColumnIndex("uploaded")
                    val uploaded = statement.getBoolean(uploadedColumn)

                    val autoTaggedColumn = statement.getColumnIndex("auto_tagged")
                    val autoTagged = statement.getBoolean(autoTaggedColumn)

                    val deletedColumn = statement.getColumnIndex("deleted")
                    val deleted = statement.getBoolean(deletedColumn)

                    val reconciledColumn = statement.getColumnIndex("reconciled")
                    val reconciled = statement.getBoolean(reconciledColumn)

                    entries.add(
                        JournalEntryV12(
                            id = id,
                            entryTime = entryTime,
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

    private data class JournalEntryV12(
        val id: String,
        val entryTime: String,
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
                "`tag` TEXT NOT NULL, " +
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
                statement.bindText(index = 3, value = conflict.entryTime)
                statement.bindText(index = 4, value = conflict.text)
                statement.bindText(index = 5, value = conflict.tag.getTag())
                statement.bindText(index = 6, value = conflict.senderName)
                statement.step()
            }
        }
    }

    private fun getExistingConflicts(connection: SQLiteConnection): List<EntryConflictV12> {
        val entries = mutableListOf<EntryConflictV12>()
        connection.prepare("SELECT * FROM EntryConflict").use { statement ->
            while (statement.step()) {
                try {
                    val idColumn = statement.getColumnIndex("id")
                    val id = statement.getText(idColumn)

                    val entryIdColumn = statement.getColumnIndex("entry_id")
                    val entryId = statement.getText(entryIdColumn)

                    val entryTimeColumn = statement.getColumnIndex("entry_time")
                    val entryTime = statement.getText(entryTimeColumn)

                    val textColumn = statement.getColumnIndex("text")
                    val text = statement.getText(textColumn)

                    val tagColumn = statement.getColumnIndex("tag")
                    val tag = statement.getText(tagColumn)

                    val senderNameColumn = statement.getColumnIndex("sender_name")
                    val senderName = statement.getText(senderNameColumn)

                    entries.add(
                        EntryConflictV12(
                            id = id,
                            entryId = entryId,
                            entryTime = entryTime,
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

    private data class EntryConflictV12(
        val id: String,
        val entryId: String,
        val entryTime: String,
        val text: String,
        val tag: String?,
        val senderName: String,
    )

    private fun String?.getTag() =
        if (this.isNullOrEmpty() || this.isBlank() || this == "null") {
            Tag.NO_TAG.value
        } else {
            this
        }
}
