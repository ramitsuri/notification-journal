package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Creates reconcile_checks table
class MigrationFrom17To18 : Migration(17, 18) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `reconcile_checks` (" +
                "`id` TEXT NOT NULL, " +
                "`tag` TEXT NOT NULL, " +
                "`days_of_week` TEXT NOT NULL, " +
                "`match_type` TEXT NOT NULL, " +
                "`query` TEXT NOT NULL, " +
                "`enabled` INTEGER NOT NULL DEFAULT 1, " +
                "PRIMARY KEY(`id`))",
        )
    }
}
