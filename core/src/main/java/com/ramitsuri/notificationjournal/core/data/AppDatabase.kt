package com.ramitsuri.notificationjournal.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom1To2
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom2To3
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom3To4
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.utils.DatabaseConverters

@Database(
    entities = [
        JournalEntry::class,
        JournalEntryTemplate::class,
        Tag::class,
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao

    abstract fun tagsDao(): TagsDao

    abstract fun templateDao(): JournalEntryTemplateDao

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
                    .addMigrations(MigrationFrom3To4())
                    .build()
            }
            return INSTANCE as AppDatabase
        }

        fun getJournalEntryDao(context: Context) = getInstance(context).journalEntryDao()

        fun getJournalEntryTemplateDao(context: Context) = getInstance(context).templateDao()

        fun getTagsDao(context: Context) = getInstance(context).tagsDao()
    }
}

