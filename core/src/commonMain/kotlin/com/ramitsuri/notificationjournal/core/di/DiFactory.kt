package com.ramitsuri.notificationjournal.core.di

import androidx.room.RoomDatabase
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.WearDataSharingClient
import com.ramitsuri.notificationjournal.core.repository.ExportRepository
import com.ramitsuri.notificationjournal.core.repository.ImportRepository
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineDispatcher
import java.nio.file.Path

expect class DiFactory {
    val allowJournalImport: Boolean

    fun getSettings(): Settings

    fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

    fun getWearDataSharingClient(): WearDataSharingClient

    fun getNotificationHandler(): NotificationHandler

    fun getDataStorePath(): Path

    fun getImportRepository(ioDispatcher: CoroutineDispatcher): ImportRepository

    fun getExportRepository(ioDispatcher: CoroutineDispatcher): ExportRepository?
}
