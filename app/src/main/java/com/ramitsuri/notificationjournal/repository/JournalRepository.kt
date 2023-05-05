package com.ramitsuri.notificationjournal.repository

import com.ramitsuri.notificationjournal.data.JournalEntry
import com.ramitsuri.notificationjournal.data.JournalEntryDao
import com.ramitsuri.notificationjournal.data.JournalEntryUpdate
import com.ramitsuri.notificationjournal.model.SortOrder
import com.ramitsuri.notificationjournal.network.Api
import okhttp3.internal.http.HTTP_OK
import java.time.Instant
import java.time.ZoneId

class JournalRepository(
    private val api: Api,
    private val dao: JournalEntryDao
) {
    suspend fun get(order: SortOrder): List<JournalEntry> {
        return when (order) {
            SortOrder.ASC -> dao.getAllAsc()

            SortOrder.DESC -> dao.getAllDesc()
        }
    }

    suspend fun edit(id: Int, text: String) {
        dao.update(
            JournalEntryUpdate(
                id = id,
                text = text
            )
        )
    }

    suspend fun insert(text: String) {
        dao.insert(
            JournalEntry(
                id = 0,
                entryTime = Instant.now(),
                timeZone = ZoneId.systemDefault(),
                text = text
            )
        )
    }

    suspend fun delete(entry: JournalEntry) {
        dao.delete(listOf(entry))
    }

    suspend fun upload(): String? {
        val entries = get(SortOrder.ASC)
        if (entries.isEmpty()) {
            return null
        }
        val error = try {
            val response = api.sendData(entries)
            if (response.code() == HTTP_OK) {
                dao.deleteAll()
                null
            } else {
                "Not successful"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            e.message
        }
        return error
    }
}