package com.ramitsuri.notificationjournal.core.di

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.WearDataSharingClient
import com.ramitsuri.notificationjournal.core.data.WearDataSharingClientImpl
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.repository.ExportRepository
import com.ramitsuri.notificationjournal.core.repository.ImportRepository
import com.ramitsuri.notificationjournal.core.utils.DataStoreKeyValueStore
import com.ramitsuri.notificationjournal.core.utils.KeyValueStoreV2
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.ramitsuri.notificationjournal.core.utils.SystemNotificationHandler
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.LocalDate
import okio.Path.Companion.toOkioPath
import kotlin.time.Instant

@ContributesTo(AppScope::class)
@BindingContainer
object AndroidCoreBindings {
    @Provides
    fun provideContext(application: Application): Context = application

    @Provides
    @SingleIn(AppScope::class)
    fun provideDatabaseBuilder(application: Application): RoomDatabase.Builder<AppDatabase> {
        val dbFile = application.getDatabasePath("app_database")
        return Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            dbFile.absolutePath,
        )
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideKeyValueStore(application: Application): KeyValueStoreV2 {
        val file = application.filesDir.resolve(DataStoreKeyValueStore.FILE).toPath()
        return DataStoreKeyValueStore(
            dataStore =
                PreferenceDataStoreFactory.createWithPath(
                    produceFile = { file.toOkioPath() },
                ),
        )
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideNotificationHandler(application: Application): NotificationHandler =
        SystemNotificationHandler(application)

    @Provides
    @SingleIn(AppScope::class)
    fun provideWearDataSharingClient(application: Application): WearDataSharingClient =
        WearDataSharingClientImpl(application)

    @Provides
    fun provideOptionalExportRepository(): ExportRepository? = null

    @Provides
    @SingleIn(AppScope::class)
    fun provideImportRepository(): ImportRepository =
        object : ImportRepository {
            override val importedEntriesFlow = emptyFlow<List<JournalEntry>>()

            override suspend fun import(
                fromDir: String,
                startDate: LocalDate,
                endDate: LocalDate,
                lastImportDate: Instant,
            ) {}
        }
}
