package com.ramitsuri.notificationjournal.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ramitsuri.notificationjournal.core.model.voice.VoiceNote
import com.ramitsuri.notificationjournal.core.model.voice.VoiceNoteStatus
import kotlinx.coroutines.flow.Flow

@Dao
abstract class VoiceNoteDao {
    @Query("SELECT * FROM voice_notes WHERE status != 'SUCCESS' ORDER BY recordedAt ASC, segmentIndex ASC")
    abstract fun getPendingOrFailedFlow(): Flow<List<VoiceNote>>

    @Query("SELECT * FROM voice_notes WHERE status = 'PROCESSING' ORDER BY recordedAt ASC, segmentIndex ASC")
    abstract suspend fun getPendingNotes(): List<VoiceNote>

    @Query("SELECT * FROM voice_notes WHERE id = :id")
    abstract suspend fun get(id: String): VoiceNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(note: VoiceNote)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(notes: List<VoiceNote>)

    @Update
    abstract suspend fun update(note: VoiceNote)

    @Query("UPDATE voice_notes SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    abstract suspend fun updateStatus(
        id: String,
        status: VoiceNoteStatus,
        errorMessage: String? = null,
    )

    @Query("DELETE FROM voice_notes WHERE id = :id")
    abstract suspend fun delete(id: String)

    @Query("DELETE FROM voice_notes")
    abstract suspend fun deleteAll()
}
