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

    @Suppress("IfThenToElvis")
    @Transaction
    open suspend fun insertOrUpdate(id: Int? = null, text: String, tag: String) {
        val idInQuestion = if (id != null) {
            id
        } else {
            (getAll().maxByOrNull { it.id }?.id ?: 0) + 1
        }
        insertOrUpdate(JournalEntryTemplate(idInQuestion, text, tag))
    }

    @Query("SELECT * FROM journalentrytemplate")
    protected abstract suspend fun getAll(): List<JournalEntryTemplate>

    @Upsert
    protected abstract suspend fun insertOrUpdate(journalEntryTemplate: JournalEntryTemplate)
}