package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MigrationFrom2To3 : Migration(2, 3) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `Tags` " +
                    "(" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`order` INTEGER NOT NULL, " +
                    "`value` TEXT NOT NULL" +
                    ")"
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_Tags_value` " +
                    "ON `Tags` (`value`)"
        )
    }
}
