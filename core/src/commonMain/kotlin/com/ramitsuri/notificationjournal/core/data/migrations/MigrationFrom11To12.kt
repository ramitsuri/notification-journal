package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Removes TimeZone column, changes entryTime to LocalDateTime from Instant in JournalEntry and EntryConflict
// NOTE: Doesn't migrate any data because not worth the effort
class MigrationFrom11To12 : Migration(11, 12) {
    override fun migrate(connection: SQLiteConnection) {
        migrateEntries(connection)
        migrateConflicts(connection)
    }

    private fun migrateEntries(connection: SQLiteConnection) {
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
                    ")"
        )
    }

    private fun migrateConflicts(connection: SQLiteConnection) {
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
                    ")"
        )
    }
}