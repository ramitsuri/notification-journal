package com.ramitsuri.notificationjournal.core.di

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ramitsuri.notificationjournal.core.BuildKonfig
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.WearDataSharingClient
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.ui.addjournal.AddJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.editjournal.EditJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.utils.DataStoreKeyValueStore
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelInfo
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.ramitsuri.notificationjournal.core.utils.NotificationInfo
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.prefs.Preferences
import kotlin.reflect.KClass

actual class Factory {
    actual fun getSettings(): Settings {
        // File located at ~/Library/Preferences/com.apple.java.util.prefs
        return PreferencesSettings(Preferences.userRoot().node(packageName))
    }

    actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        Files.createDirectories(Paths.get(appDir))
        val fileName = "database"
        val dbFile = File(appDir, fileName)
        return Room.databaseBuilder<AppDatabase>(
            name = dbFile.absolutePath,
        )
    }

    actual fun getWearDataSharingClient(): WearDataSharingClient {
        return object : WearDataSharingClient {
            override suspend fun postJournalEntry(
                value: String,
                time: Instant,
                timeZoneId: TimeZone,
                tag: String?
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
                handle: SavedStateHandle
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
                handle: SavedStateHandle
            ): T {
                return getVMInstance(handle) as T
            }
        }
    }

    actual fun getDataStorePath(): String {
        return File(appDir, DataStoreKeyValueStore.FILE).absolutePath
    }

    companion object {
        private val packageName = if (BuildKonfig.IS_DEBUG) {
            "com.ramitsuri.notificationjournal.debug"
        } else {
            "com.ramitsuri.notificationjournal.release"
        }

        // TODO make compatible with other desktop OSs
        private val appDir = System.getProperty("user.home")
            .plus("/Library")
            .plus("/com.ramitsuri.notificationjournal")
            .plus(
                if (BuildKonfig.IS_DEBUG) {
                    "/debug"
                } else {
                    "/release"
                }
            )
    }
}