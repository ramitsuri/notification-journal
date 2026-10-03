package com.ramitsuri.notificationjournal.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.SQLiteException
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.TagOrderUpdate
import com.ramitsuri.notificationjournal.core.model.TagTextUpdate
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TagsDao {
    @Transaction
    open suspend fun updateOrder(tags: List<Tag>) {
        tags.forEach { tag ->
            updateOrder(TagOrderUpdate(id = tag.id, order = tag.order))
        }
    }

    @Transaction
    open suspend fun deleteIfPossible(tag: Tag) {
        delete(tag)
    }

    @Transaction
    open suspend fun insertIfPossible(tag: Tag): Boolean {
        return try {
            insert(tag)
            true
        } catch (e: SQLiteException) {
            false
        }
    }

    @Transaction
    open suspend fun updateTextIfPossible(tagTextUpdate: TagTextUpdate): Boolean {
        return try {
            updateText(tagTextUpdate)
            true
        } catch (e: SQLiteException) {
            false
        }
    }

    @Transaction
    open suspend fun clearAndInsert(tags: List<Tag>) {
        deleteAll()
        tags.forEach {
            insert(it)
        }
    }

    @Query("SELECT * FROM tags ORDER BY `order` ASC")
    abstract fun getAllFlow(): Flow<List<Tag>>

    @Query("SELECT * FROM tags ORDER BY `order` ASC")
    abstract suspend fun getAll(): List<Tag>

    @Update(entity = Tag::class)
    protected abstract suspend fun updateText(tagTextUpdate: TagTextUpdate)

    @Insert
    protected abstract suspend fun insert(tag: Tag)

    @Delete
    protected abstract suspend fun delete(tag: Tag)

    @Query("DELETE FROM tags")
    protected abstract suspend fun deleteAll()

    @Update(entity = Tag::class)
    protected abstract suspend fun updateOrder(tagOrderUpdate: TagOrderUpdate)
}
