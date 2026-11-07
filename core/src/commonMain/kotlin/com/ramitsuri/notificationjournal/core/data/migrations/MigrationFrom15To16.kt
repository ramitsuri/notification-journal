package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Creates logs table
class MigrationFrom15To16 : Migration(15, 16) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `logs` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`time` TEXT NOT NULL, " +
                "`severity` TEXT NOT NULL, " +
                "`message` TEXT NOT NULL, " +
                "`tag` TEXT NOT NULL, " +
                "`errorMessage` TEXT, " +
                "`stackTrace` TEXT)",
        )
    }
}
