package com.ramitsuri.notificationjournal.core.di

import androidx.room.RoomDatabase
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.DataSharingClient
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.russhwolf.settings.Settings

expect class Factory {
    fun getSettings(): Settings

    fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

    fun getDataSharingClient(): DataSharingClient

    fun getNotificationHandler(): NotificationHandler

    fun isDebug(): Boolean
}
