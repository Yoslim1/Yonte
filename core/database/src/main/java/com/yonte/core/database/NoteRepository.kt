package com.yonte.core.database

import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.flow.Flow
import java.util.UUID

object ArabicNormalizer {
    fun normalize(value: String): String = value
        .lowercase()
        .replace("أ", "ا")
        .replace("إ", "ا")
        .replace("آ", "ا")
        .replace("ٱ", "ا")
        .replace("ؤ", "ء")
        .replace("ئ", "ء")
        .replace("ة", "ه")
        .replace(Regex("[ًٌٍَُِّْـ]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

class NoteRepository(private val database: YonteDatabase) {
    private val dao = database.noteDao()

    fun observeActive(): Flow<List<NoteEntity>> = dao.observeActive()

    suspend fun get(id: String): NoteEntity? = dao.getById(id)

    suspend fun save(id: String?, title: String, body: String): NoteEntity {
        val now = System.currentTimeMillis()
        val existing = id?.let { dao.getById(it) }
        val note = NoteEntity(
            id = existing?.id ?: id ?: UUID.randomUUID().toString(),
            title = title.trim(),
            body = body,
            normalizedText = ArabicNormalizer.normalize("$title $body"),
            isPinned = existing?.isPinned ?: false,
            isArchived = existing?.isArchived ?: false,
            isTrashed = existing?.isTrashed ?: false,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        dao.upsert(note)
        updateFts(note)
        return note
    }

    suspend fun setPinned(id: String, pinned: Boolean) {
        dao.setPinned(id, pinned, System.currentTimeMillis())
    }

    suspend fun setArchived(id: String, archived: Boolean) {
        dao.setArchived(id, archived, System.currentTimeMillis())
    }

    suspend fun setTrashed(id: String, trashed: Boolean) {
        dao.setTrashed(id, trashed, System.currentTimeMillis())
    }

    suspend fun search(query: String): List<NoteEntity> {
        val normalized = ArabicNormalizer.normalize(query)
        if (normalized.isBlank()) return dao.searchFallback("")
        return try {
            val escaped = normalized.replace("'", "''")
            val rows = database.openHelper.readableDatabase.query(
                SimpleSQLiteQuery("SELECT note_id FROM notes_fts WHERE notes_fts MATCH ?", arrayOf("$escaped*"))
            )
            val ids = buildList {
                while (rows.moveToNext()) add(rows.getString(0))
            }
            rows.close()
            if (ids.isEmpty()) emptyList() else dao.getActiveByIds(ids)
        } catch (_: Exception) {
            dao.searchFallback(normalized)
        }
    }

    private fun updateFts(note: NoteEntity) {
        try {
            val db = database.openHelper.writableDatabase
            db.execSQL("DELETE FROM notes_fts WHERE note_id = ?", arrayOf(note.id))
            if (!note.isTrashed && !note.isArchived) {
                db.execSQL("INSERT INTO notes_fts(note_id, title, body) VALUES (?, ?, ?)", arrayOf(note.id, note.title, note.normalizedText))
            }
        } catch (_: Exception) {
            // Fallback search remains available on SQLite builds without FTS5.
        }
    }
}
