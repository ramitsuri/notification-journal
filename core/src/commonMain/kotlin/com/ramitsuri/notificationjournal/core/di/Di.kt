package com.ramitsuri.notificationjournal.core.di

import androidx.room.RoomDatabase
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.russhwolf.settings.Settings

expect class Factory {
    fun getSettings(): Settings

    fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>
}
