package com.ramitsuri.notificationjournal.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom1To2
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom2To3
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom3To4
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom4To5
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom5To6
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom6To7
import com.ramitsuri.notificationjournal.core.di.Factory
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.utils.DatabaseConverters
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        JournalEntry::class,
        JournalEntryTemplate::class,
        Tag::class,
    ],
    version = 7,
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

        private fun getInstance(factory: Factory): AppDatabase {
            if (INSTANCE == null) {
                INSTANCE = factory
                    .getDatabaseBuilder()
                    .setDriver(BundledSQLiteDriver())
                    .setQueryCoroutineContext(Dispatchers.IO)
                    .addMigrations(MigrationFrom1To2())
                    .addMigrations(MigrationFrom2To3())
                    .addMigrations(MigrationFrom3To4())
                    .addMigrations(MigrationFrom4To5())
                    .addMigrations(MigrationFrom5To6())
                    .addMigrations(MigrationFrom6To7())
                    .build()
            }
            return INSTANCE as AppDatabase
        }

        fun getJournalEntryDao(factory: Factory) = getInstance(factory).journalEntryDao()

        fun getJournalEntryTemplateDao(factory: Factory) = getInstance(factory).templateDao()

        fun getTagsDao(factory: Factory) = getInstance(factory).tagsDao()
    }
}

