package com.ramitsuri.notificationjournal.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.TagOrderUpdate
import com.ramitsuri.notificationjournal.core.model.TagTextUpdate
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TagsDao {
    @Transaction
    open suspend fun updateOrder(tags: List<Tag>) {
        tags.map { tag ->
            updateOrder(TagOrderUpdate(id = tag.id, order = tag.order))
        }
    }

    @Transaction
    open suspend fun deleteIfPossible(tag: Tag): Boolean {
        val rowCountForTag = getRowCountForTag(tag.value)
        if (rowCountForTag != 0) {
            return false
        }
        delete(tag)
        return true
    }

    @Query("SELECT * FROM tags ORDER BY `order` ASC")
    abstract fun getAllFlow(): Flow<List<Tag>>

    @Query("SELECT * FROM tags ORDER BY `order` ASC")
    abstract suspend fun getAll(): List<Tag>

    @Insert
    abstract suspend fun insert(tag: Tag)

    @Update(entity = Tag::class)
    abstract suspend fun updateText(tagTextUpdate: TagTextUpdate)

    @Delete
    protected abstract suspend fun delete(tag: Tag)

    @Query("SELECT COUNT(id) from journalentry WHERE tag = :tag")
    protected abstract suspend fun getRowCountForTag(tag: String): Int

    @Update(entity = Tag::class)
    protected abstract suspend fun updateOrder(tagOrderUpdate: TagOrderUpdate)
}