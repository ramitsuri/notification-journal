package com.ramitsuri.notificationjournal.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.coroutines.flow.Flow

@Dao
abstract class JournalEntryTemplateDao {
    @Query("SELECT * FROM journalentrytemplate")
    abstract fun getAllFlow(): Flow<List<JournalEntryTemplate>>

    @Query("DELETE FROM journalentrytemplate")
    abstract suspend fun deleteAll()

    @Delete
    abstract suspend fun delete(journalEntries: List<JournalEntryTemplate>)

    @Insert
    abstract suspend fun insert(journalEntryTemplate: JournalEntryTemplate)

    @Transaction
    open suspend fun insertOrUpdate(id: String? = null, text: String, tag: String) {
        val template = if (id != null) {
            JournalEntryTemplate(id = id, text = text, tag = tag)
        } else {
            JournalEntryTemplate(text = text, tag = tag)
        }
        insertOrUpdate(template)
    }

    @Query("SELECT * FROM journalentrytemplate")
    abstract suspend fun getAll(): List<JournalEntryTemplate>

    @Upsert
    protected abstract suspend fun insertOrUpdate(journalEntryTemplate: JournalEntryTemplate)
}