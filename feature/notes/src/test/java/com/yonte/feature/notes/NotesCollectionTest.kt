package com.yonte.feature.notes

import com.yonte.core.database.ArabicNormalizer
import com.yonte.core.database.NoteEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class NotesCollectionTest {
    private fun note(id: String, title: String, archived: Boolean = false, trashed: Boolean = false) = NoteEntity(
        id = id, title = title, body = "", normalizedText = ArabicNormalizer.normalize(title),
        isArchived = archived, isTrashed = trashed, createdAt = 0, updatedAt = 0,
    )

    @Test
    fun trashTakesPrecedenceOverArchiveAndActive() {
        val notes = listOf(note("active", "idea"), note("archive", "idea", archived = true), note("trash", "idea", archived = true, trashed = true))
        assertEquals(listOf("active"), filterNotes(notes, NotesCollection.ACTIVE, "").map { it.id })
        assertEquals(listOf("archive"), filterNotes(notes, NotesCollection.ARCHIVED, "").map { it.id })
        assertEquals(listOf("trash"), filterNotes(notes, NotesCollection.TRASHED, "").map { it.id })
    }

    @Test
    fun arabicSearchNormalizesDiacriticsAndAlefInArchivedNotes() {
        val notes = listOf(note("archive", "أَفكار المستقبل", archived = true))
        assertEquals(notes, filterNotes(notes, NotesCollection.ARCHIVED, "افكار مستقبل"))
        assertEquals(emptyList<NoteEntity>(), filterNotes(notes, NotesCollection.ACTIVE, "افكار"))
    }

    @Test
    fun filterReflectsUpdatedSnapshotWhileSearchIsActive() {
        val active = note("1", "Future")
        assertEquals(listOf(active), filterNotes(listOf(active), NotesCollection.ACTIVE, "future"))
        assertEquals(emptyList<NoteEntity>(), filterNotes(listOf(active.copy(isTrashed = true)), NotesCollection.ACTIVE, "future"))
    }
}
