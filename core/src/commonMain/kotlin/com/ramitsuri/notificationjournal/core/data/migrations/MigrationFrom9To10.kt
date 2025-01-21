package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Creates EntryConflict table
class MigrationFrom9To10 : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `EntryConflict` " +
                "(" +
                "`id` TEXT NOT NULL, " +
                "`entry_id` TEXT NOT NULL, " +
                "`entry_time` INTEGER NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "`tag` TEXT, " +
                "`sender_name` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`)" +
                ")",
        )
    }
}
