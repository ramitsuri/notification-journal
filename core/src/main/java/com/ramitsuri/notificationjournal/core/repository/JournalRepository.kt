package com.ramitsuri.notificationjournal.core.repository

import com.ramitsuri.notificationjournal.core.data.JournalEntry
import com.ramitsuri.notificationjournal.core.data.JournalEntryDao
import com.ramitsuri.notificationjournal.core.data.JournalEntryUpdate
import com.ramitsuri.notificationjournal.core.model.SortOrder
import com.ramitsuri.notificationjournal.core.network.Api
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.internal.http.HTTP_OK
import java.time.Instant
import java.time.ZoneId

class JournalRepository(
    private val api: Api,
    private val dao: JournalEntryDao
) {
    fun getFlow(order: SortOrder): Flow<List<JournalEntry>> {
        return dao.getAllFlow().map { list ->
            when (order) {
                SortOrder.ASC -> list.sortedBy { it.entryTime }

                SortOrder.DESC -> list.sortedByDescending { it.entryTime }
            }
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
        val entries = dao.getAll()
        if (entries.isEmpty()) {
            return null
        }
        return try {
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
    }
}