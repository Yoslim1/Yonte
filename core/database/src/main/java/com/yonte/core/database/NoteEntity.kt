package com.yonte.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val normalizedText: String,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

data class NoteSearchRow(
    val note_id: String,
)
