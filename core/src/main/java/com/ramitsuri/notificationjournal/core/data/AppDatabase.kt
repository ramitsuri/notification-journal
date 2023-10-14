package com.ramitsuri.notificationjournal.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom1To2
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom2To3
import com.ramitsuri.notificationjournal.core.model.JournalEntry
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.utils.DatabaseConverters

@Database(
    entities = [
        JournalEntry::class,
        Tag::class,
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun getInstance(context: Context): AppDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room
                    .databaseBuilder(
                        context,
                        AppDatabase::class.java,
                        "app_database"
                    )
                    .addMigrations(MigrationFrom1To2())
                    .addMigrations(MigrationFrom2To3())
                    .build()
            }
            return INSTANCE as AppDatabase
        }

        fun getDao(context: Context) = getInstance(context).journalEntryDao()
    }
}

