package com.ramitsuri.notificationjournal.core.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.DataSharingClient
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelInfo
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.ramitsuri.notificationjournal.core.utils.NotificationInfo
import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import java.io.File
import java.util.Properties

actual class Factory {
    actual fun getSettings(): Settings {
        return PropertiesSettings(Properties())
    }

    actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        val dbFile = File(System.getProperty("java.io.tmpdir"), "app_database")
        return Room.databaseBuilder<AppDatabase>(
            name = dbFile.absolutePath,
        )
    }

    actual fun getDataSharingClient(): DataSharingClient {
        return object : DataSharingClient {
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

            override suspend fun postTemplate(id: Int, value: String, tag: String): Boolean {
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

    actual fun isDebug(): Boolean {
        return false
    }
}