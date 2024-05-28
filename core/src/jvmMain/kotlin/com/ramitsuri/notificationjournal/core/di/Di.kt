package com.ramitsuri.notificationjournal.core.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
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
}