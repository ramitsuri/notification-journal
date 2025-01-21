package com.ramitsuri.notificationjournal.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
abstract class JournalEntryTemplateDao {
    @Query("SELECT * FROM journalentrytemplate")
    abstract fun getAllFlow(): Flow<List<JournalEntryTemplate>>

    @Delete
    abstract suspend fun delete(journalEntries: List<JournalEntryTemplate>)

    @Transaction
    open suspend fun clearAndInsert(templates: List<JournalEntryTemplate>) {
        deleteAll()
        insert(templates)
    }

    @Transaction
    open suspend fun insert(templates: List<JournalEntryTemplate>) {
        templates.forEach {
            insert(it)
        }
    }

    @Transaction
    open suspend fun insertOrUpdate(
        id: String? = null,
        text: String,
        tag: String,
        shortDisplayText: String,
        displayText: String,
    ) {
        val template =
            JournalEntryTemplate(
                id = id ?: UUID.randomUUID().toString(),
                text = text,
                tag = tag,
                shortDisplayText = shortDisplayText,
                displayText = displayText,
            )
        insertOrUpdate(template)
    }

    @Query("SELECT * FROM journalentrytemplate")
    abstract suspend fun getAll(): List<JournalEntryTemplate>

    @Upsert
    protected abstract suspend fun insertOrUpdate(journalEntryTemplate: JournalEntryTemplate)

    @Query("DELETE FROM journalentrytemplate")
    abstract suspend fun deleteAll()

    @Insert
    protected abstract suspend fun insert(journalEntryTemplate: JournalEntryTemplate)
}
