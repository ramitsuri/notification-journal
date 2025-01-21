package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Creates Dictionary table
class MigrationFrom10To11 : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `DictionaryItem` " +
                "(" +
                "`id` TEXT NOT NULL, " +
                "`word` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`)" +
                ")",
        )
    }
}
