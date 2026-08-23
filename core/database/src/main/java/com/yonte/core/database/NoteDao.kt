package com.yonte.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeActive(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND isArchived = 0 AND (title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' OR normalizedText LIKE '%' || :query || '%') ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun searchFallback(query: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id IN (:ids) AND isTrashed = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getActiveByIds(ids: List<String>): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Update
    suspend fun update(note: NoteEntity)

    @Query("UPDATE notes SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET isArchived = :archived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET isTrashed = :trashed, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setTrashed(id: String, trashed: Boolean, updatedAt: Long)

    @Query("DELETE FROM notes WHERE isTrashed = 1")
    suspend fun emptyTrash()
}
