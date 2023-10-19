package com.ramitsuri.notificationjournal.core.repository

import com.ramitsuri.notificationjournal.core.data.JournalEntryDao
import com.ramitsuri.notificationjournal.core.model.JournalEntry
import com.ramitsuri.notificationjournal.core.model.JournalEntryTagUpdate
import com.ramitsuri.notificationjournal.core.model.JournalEntryTextUpdate
import com.ramitsuri.notificationjournal.core.model.JournalEntryTimeUpdate
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

    suspend fun get(id: Int): JournalEntry {
        return dao.get(id)
    }

    suspend fun editText(id: Int, text: String) {
        dao.updateText(
            JournalEntryTextUpdate(
                id = id,
                text = text
            )
        )
    }

    suspend fun editTag(id: Int, tag: String?) {
        dao.updateTag(
            JournalEntryTagUpdate(
                id = id,
                tag = tag
            )
        )
    }

    suspend fun editEntryTime(id: Int, time: Instant?) {
        dao.updateEntryTime(
            JournalEntryTimeUpdate(
                id = id,
                entryTimeOverride = time,
            )
        )
    }

    suspend fun insert(text: String, tag: String? = null, originalEntryTime: Instant? = null) {
        val entryTime = Instant.now()
        val timeZone = ZoneId.systemDefault()
        text
            .split("\n")
            .forEach {
                dao.insert(
                    JournalEntry(
                        id = 0,
                        entryTime = entryTime,
                        timeZone = timeZone,
                        text = it.trim(),
                        tag = tag,
                        entryTimeOverride = originalEntryTime
                    )
                )
            }
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