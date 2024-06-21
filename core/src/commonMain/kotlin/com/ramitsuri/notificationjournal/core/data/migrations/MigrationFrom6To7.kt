package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Adds 2 new columns "deleted" and "reconciled" to "JournalEntry" table
class MigrationFrom6To7 : Migration(6, 7) {

    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE 'JournalEntry' " +
                    "ADD COLUMN `deleted` INTEGER NOT NULL DEFAULT 0"
        )

        connection.execSQL(
            "ALTER TABLE 'JournalEntry' " +
                    "ADD COLUMN `reconciled` INTEGER NOT NULL DEFAULT 0"
        )
    }
}