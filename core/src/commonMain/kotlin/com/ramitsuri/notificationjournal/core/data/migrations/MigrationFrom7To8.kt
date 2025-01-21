package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Adds 2 columns "display_text" and "short_display_text" to the JournalEntryTemplate table
class MigrationFrom7To8 : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE 'JournalEntryTemplate' " +
                "ADD COLUMN `display_text` TEXT NOT NULL DEFAULT \"\"",
        )

        connection.execSQL(
            "ALTER TABLE 'JournalEntryTemplate' " +
                "ADD COLUMN `short_display_text` TEXT NOT NULL DEFAULT \"\"",
        )
    }
}
