package com.ramitsuri.notificationjournal.core.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.WearDataSharingClient
import com.ramitsuri.notificationjournal.core.data.WearDataSharingClientImpl
import com.ramitsuri.notificationjournal.core.repository.ExportRepository
import com.ramitsuri.notificationjournal.core.repository.ImportRepository
import com.ramitsuri.notificationjournal.core.utils.DataStoreKeyValueStore
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.ramitsuri.notificationjournal.core.utils.SystemNotificationHandler
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.CoroutineDispatcher
import java.nio.file.Path

actual class DiFactory(private val application: Application) {
    actual val allowJournalImport: Boolean = false

    actual fun getSettings(): Settings {
        return SharedPreferencesSettings(
            application.getSharedPreferences(
                "com.ramitsuri.notificationjournal.prefs",
                Context.MODE_PRIVATE,
            ),
        )
    }

    actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        val dbFile = application.getDatabasePath("app_database")
        return Room
            .databaseBuilder(
                application,
                AppDatabase::class.java,
                dbFile.absolutePath,
            )
    }

    actual fun getWearDataSharingClient(): WearDataSharingClient {
        return WearDataSharingClientImpl(application)
    }

    actual fun getNotificationHandler(): NotificationHandler {
        return SystemNotificationHandler(application)
    }

    actual fun getDataStorePath(): Path {
        return application.filesDir.resolve(DataStoreKeyValueStore.FILE).toPath()
    }

    actual fun getImportRepository(ioDispatcher: CoroutineDispatcher): ImportRepository {
        error("Not supported on Android")
    }

    actual fun getExportRepository(ioDispatcher: CoroutineDispatcher): ExportRepository? {
        return null
    }
}
