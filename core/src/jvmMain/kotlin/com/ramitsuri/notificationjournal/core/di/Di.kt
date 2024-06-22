package com.ramitsuri.notificationjournal.core.di

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.WearDataSharingClient
import com.ramitsuri.notificationjournal.core.ui.addjournal.AddJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.editjournal.EditJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelInfo
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.ramitsuri.notificationjournal.core.utils.NotificationInfo
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import java.io.File
import java.util.prefs.Preferences
import kotlin.reflect.KClass

actual class Factory {
    actual fun getSettings(): Settings {
        return PreferencesSettings(Preferences.userRoot().node("com.ramitsuri.notificationjournal"))
    }

    actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        val dbFile = File(System.getProperty("java.io.tmpdir"), "app_database")
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

            override suspend fun postTemplate(id: String, value: String, tag: String): Boolean {
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
}