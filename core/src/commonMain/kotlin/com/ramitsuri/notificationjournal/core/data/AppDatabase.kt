package com.ramitsuri.notificationjournal.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ramitsuri.notificationjournal.core.data.dictionary.DictionaryDao
import com.ramitsuri.notificationjournal.core.data.dictionary.DictionaryItem
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom10To11
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom11To12
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom12To13
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom13To14
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom1To2
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom2To3
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom3To4
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom4To5
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom5To6
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom6To7
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom7To8
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom8To9
import com.ramitsuri.notificationjournal.core.data.migrations.MigrationFrom9To10
import com.ramitsuri.notificationjournal.core.model.EntryConflict
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
        EntryConflict::class,
        DictionaryItem::class,
    ],
    version = 14,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao

    abstract fun tagsDao(): TagsDao

    abstract fun templateDao(): JournalEntryTemplateDao

    abstract fun entryConflictDao(): EntryConflictDao

    abstract fun dictionaryDao(): DictionaryDao

    companion object {
        fun getDatabase(builder: () -> Builder<AppDatabase>): AppDatabase {
            return builder()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .addMigrations(MigrationFrom1To2())
                .addMigrations(MigrationFrom2To3())
                .addMigrations(MigrationFrom3To4())
                .addMigrations(MigrationFrom4To5())
                .addMigrations(MigrationFrom5To6())
                .addMigrations(MigrationFrom6To7())
                .addMigrations(MigrationFrom7To8())
                .addMigrations(MigrationFrom8To9())
                .addMigrations(MigrationFrom9To10())
                .addMigrations(MigrationFrom10To11())
                .addMigrations(MigrationFrom11To12())
                .addMigrations(MigrationFrom12To13())
                .addMigrations(MigrationFrom13To14())
                .build()
        }
    }
}
