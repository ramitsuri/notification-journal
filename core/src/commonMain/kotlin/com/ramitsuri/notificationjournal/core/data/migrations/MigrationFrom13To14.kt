package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Renames auto_tagged column to replaces_local in JournalEntry table
class MigrationFrom13To14 : Migration(13, 14) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE JournalEntry " +
                "RENAME COLUMN auto_tagged TO replaces_local",
        )
    }
}
