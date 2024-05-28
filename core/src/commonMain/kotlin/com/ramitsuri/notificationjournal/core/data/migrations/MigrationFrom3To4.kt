package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

class MigrationFrom3To4 : Migration(3, 4) {

    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `JournalEntryTemplate` " +
                    "(" +
                    "`id` INTEGER PRIMARY KEY NOT NULL, " +
                    "`text` TEXT NOT NULL, " +
                    "`tag` TEXT NOT NULL" +
                    ")"
        )
    }
}
