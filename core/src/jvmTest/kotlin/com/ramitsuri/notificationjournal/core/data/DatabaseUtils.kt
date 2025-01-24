package com.ramitsuri.notificationjournal.core.data

import androidx.room.Room
import androidx.room.execSQL
import androidx.room.useWriterConnection
import java.nio.file.Files
import java.nio.file.Paths

internal fun getTestDb() =
    Files
        .createDirectories(Paths.get("temp"))
        .resolve("database").toFile()
        .let { dbFile ->
            AppDatabase.getInstance {
                Room.databaseBuilder<AppDatabase>(
                    name = dbFile.absolutePath,
                )
            }
        }

internal suspend fun clearDb() {
    getTestDb().useWriterConnection {
        it.execSQL("DELETE FROM JournalEntry")
        it.execSQL("DELETE FROM JournalEntryTemplate")
        it.execSQL("DELETE FROM Tags")
        it.execSQL("DELETE FROM EntryConflict")
        it.execSQL("DELETE FROM DictionaryItem")
    }
}
