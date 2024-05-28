package com.ramitsuri.notificationjournal.core.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual class Factory(private val application: Application) {
    actual fun getSettings(): Settings {
        return SharedPreferencesSettings(
            application.getSharedPreferences(
                Constants.PREF_FILE,
                Context.MODE_PRIVATE
            )
        )
    }

    actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        val dbFile = application.getDatabasePath("app_database")
        return Room
            .databaseBuilder(
                application,
                AppDatabase::class.java,
                dbFile.absolutePath
            )
    }
}