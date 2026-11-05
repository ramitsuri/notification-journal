package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Creates voice_notes table
class MigrationFrom14To15 : Migration(14, 15) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `voice_notes` (" +
                "`id` TEXT NOT NULL, " +
                "`filePath` TEXT NOT NULL, " +
                "`recordedAt` TEXT NOT NULL, " +
                "`segmentIndex` INTEGER NOT NULL, " +
                "`status` TEXT NOT NULL, " +
                "`errorMessage` TEXT, " +
                "PRIMARY KEY(`id`))",
        )
    }
}
