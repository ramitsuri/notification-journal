package com.ramitsuri.notificationjournal.core.di

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ramitsuri.notificationjournal.core.BuildKonfig
import com.ramitsuri.notificationjournal.core.ExportRepositoryImpl
import com.ramitsuri.notificationjournal.core.ImportRepositoryImpl
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.WearDataSharingClient
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.repository.ExportRepository
import com.ramitsuri.notificationjournal.core.repository.ImportRepository
import com.ramitsuri.notificationjournal.core.ui.addjournal.AddJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.editjournal.EditJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.journalentryday.ViewJournalEntryDayViewModel
import com.ramitsuri.notificationjournal.core.utils.DataStoreKeyValueStore
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelInfo
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.ramitsuri.notificationjournal.core.utils.NotificationInfo
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.datetime.LocalDateTime
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.prefs.Preferences
import kotlin.reflect.KClass

actual class DiFactory {
    actual val allowJournalImport: Boolean = true

    actual fun getSettings(): Settings {
        // File located at ~/Library/Preferences/com.apple.java.util.prefs
        return PreferencesSettings(Preferences.userRoot().node(packageName))
    }

    actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        Files.createDirectories(appDir)
        val fileName = "database"
        val dbFile = appDir.resolve(fileName).toFile()
        return Room.databaseBuilder<AppDatabase>(
            name = dbFile.absolutePath,
        )
    }

    actual fun getWearDataSharingClient(): WearDataSharingClient {
        return object : WearDataSharingClient {
            override suspend fun postJournalEntry(
                value: String,
                time: LocalDateTime,
                tag: String?,
            ): Boolean {
                // Not supported
                return true
            }

            override suspend fun requestUpload() {
                // Not supported
            }

            override suspend fun postTemplate(template: JournalEntryTemplate): Boolean {
                // Not supported
                return true
            }

            override suspend fun clearTemplates(): Boolean {
                // Not supported
                return true
            }

            override suspend fun updateTile(): Boolean {
                // Not supported
                return true
            }
        }
    }

    actual fun getNotificationHandler(): NotificationHandler {
        return object : NotificationHandler {
            override fun init(channels: List<NotificationChannelInfo>) {
                // Not supported
            }

            override fun showNotification(notificationInfo: NotificationInfo) {
                // Not supported
            }

            override fun getNotificationId(notificationInfo: NotificationInfo): Int {
                // Not supported
                return 1
            }
        }
    }

    actual fun addJournalEntryVMFactory(
        navBackStackEntry: NavBackStackEntry,
        getVMInstance: (SavedStateHandle) -> AddJournalEntryViewModel,
    ): AbstractSavedStateViewModelFactory {
        return object : AbstractSavedStateViewModelFactory(
            owner = navBackStackEntry,
            defaultArgs = navBackStackEntry.arguments,
        ) {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                key: String,
                modelClass: KClass<T>,
                handle: SavedStateHandle,
            ): T {
                return getVMInstance(handle) as T
            }
        }
    }

    actual fun editJournalEntryVMFactory(
        navBackStackEntry: NavBackStackEntry,
        getVMInstance: (SavedStateHandle) -> EditJournalEntryViewModel,
    ): AbstractSavedStateViewModelFactory {
        return object : AbstractSavedStateViewModelFactory(
            owner = navBackStackEntry,
            defaultArgs = navBackStackEntry.arguments,
        ) {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                key: String,
                modelClass: KClass<T>,
                handle: SavedStateHandle,
            ): T {
                return getVMInstance(handle) as T
            }
        }
    }

    actual fun viewJournalEntryDayVMFactory(
        navBackStackEntry: NavBackStackEntry,
        getVMInstance: (SavedStateHandle) -> ViewJournalEntryDayViewModel,
    ): AbstractSavedStateViewModelFactory {
        return object : AbstractSavedStateViewModelFactory(
            owner = navBackStackEntry,
            defaultArgs = navBackStackEntry.arguments,
        ) {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                key: String,
                modelClass: KClass<T>,
                handle: SavedStateHandle,
            ): T {
                return getVMInstance(handle) as T
            }
        }
    }

    actual fun getDataStorePath(): Path {
        return appDir.resolve(DataStoreKeyValueStore.FILE)
    }

    actual fun getImportRepository(ioDispatcher: CoroutineDispatcher): ImportRepository {
        return ImportRepositoryImpl(ioDispatcher)
    }

    actual fun getExportRepository(ioDispatcher: CoroutineDispatcher): ExportRepository? {
        return ExportRepositoryImpl(ioDispatcher)
    }

    actual fun getJournalEntryScreenDeepLinks(): List<NavDeepLink> {
        return emptyList()
    }

    companion object {
        private val packageName =
            if (BuildKonfig.IS_DEBUG) {
                "com.ramitsuri.notificationjournal.debug"
            } else {
                "com.ramitsuri.notificationjournal.release"
            }

        // TODO make compatible with other desktop OSs
        private val appDir =
            System.getProperty("user.home")
                .let { userHomePath ->
                    val osPath =
                        System
                            .getProperty("os.name", "generic")
                            .lowercase()
                            .let { os ->
                                if ((os.indexOf("mac") >= 0) || (os.indexOf("darwin") >= 0)) {
                                    "Library"
                                } else if (os.indexOf("win") >= 0) {
                                    "Documents"
                                } else {
                                    error("OS not supported")
                                }
                            }
                    val appPath = "com.ramitsuri.notificationjournal"
                    val buildPath = if (BuildKonfig.IS_DEBUG) "debug" else "release"
                    Paths.get(userHomePath, osPath, appPath, buildPath)
                }
    }
}
