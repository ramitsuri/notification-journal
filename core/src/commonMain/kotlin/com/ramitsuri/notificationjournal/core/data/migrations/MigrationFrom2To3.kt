package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

class MigrationFrom2To3 : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `Tags` " +
                "(" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`order` INTEGER NOT NULL, " +
                "`value` TEXT NOT NULL" +
                ")",
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_Tags_value` " +
                "ON `Tags` (`value`)",
        )
    }
}
