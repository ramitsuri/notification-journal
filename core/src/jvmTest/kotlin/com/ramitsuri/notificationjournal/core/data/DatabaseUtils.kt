package com.ramitsuri.notificationjournal.core.data

import androidx.room.Room

internal fun getTestDb() =
    AppDatabase.getDatabase {
        Room.inMemoryDatabaseBuilder()
    }
