package com.yonte.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yonte.core.database.ArabicNormalizer
import com.yonte.core.database.NoteEntity
import com.yonte.core.database.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class EditorSession(val id: String, val note: NoteEntity?, val title: String, val body: String)

enum class NotesCollection { ACTIVE, ARCHIVED, TRASHED }

internal fun filterNotes(notes: List<NoteEntity>, collection: NotesCollection, query: String): List<NoteEntity> {
    val terms = ArabicNormalizer.normalize(query).split(' ').filter { it.isNotBlank() }
    return notes.filter { note ->
        val inCollection = when (collection) {
            NotesCollection.ACTIVE -> !note.isTrashed && !note.isArchived
            NotesCollection.ARCHIVED -> !note.isTrashed && note.isArchived
            NotesCollection.TRASHED -> note.isTrashed
        }
        inCollection && terms.all { note.normalizedText.contains(it) }
    }
}

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository,
) : ViewModel() {
    private val activeEditor = MutableStateFlow<EditorSession?>(null)
    internal val editor = activeEditor.asStateFlow()
    internal fun openEditor(note: NoteEntity? = null, text: String = "") {
        activeEditor.value = EditorSession(note?.id ?: java.util.UUID.randomUUID().toString(), note, note?.title.orEmpty(), note?.body ?: text)
    }
    internal fun updateEditor(title: String, body: String) {
        activeEditor.value = activeEditor.value?.copy(title = title, body = body)
    }
    internal fun closeEditor(id: String?) { if (activeEditor.value?.id == id) activeEditor.value = null }

    private val query = MutableStateFlow("")
    private val selectedCollection = MutableStateFlow(NotesCollection.ACTIVE)
    val collection: StateFlow<NotesCollection> = selectedCollection.asStateFlow()
    private val failedSave = MutableStateFlow(false)
    val saveFailed: StateFlow<Boolean> = failedSave.asStateFlow()
    private var draftSaveJob: Job? = null
    private val saveMutex = Mutex()
    private var saveVersion = 0L

    val notes: StateFlow<List<NoteEntity>> = combine(repository.observeAll(), selectedCollection, query) { notes, selected, value ->
        filterNotes(notes, selected, value)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun search(value: String) { query.value = value }

    fun selectCollection(value: NotesCollection) { selectedCollection.value = value }

    fun autosave(id: String?, title: String, body: String, onSaved: (NoteEntity) -> Unit = {}, onComplete: () -> Unit = {}) {
        scheduleSave(id, title, body, 350, onSaved, onComplete)
    }

    fun saveImmediately(id: String?, title: String, body: String, onSaved: (NoteEntity) -> Unit = {}, onComplete: () -> Unit = {}) {
        scheduleSave(id, title, body, 0, onSaved, onComplete)
    }

    private fun scheduleSave(id: String?, title: String, body: String, delayMillis: Long, onSaved: (NoteEntity) -> Unit, onComplete: () -> Unit = {}) {
        draftSaveJob?.cancel()
        val version = ++saveVersion
        failedSave.value = false
        draftSaveJob = viewModelScope.launch {
            try {
                delay(delayMillis)
                saveMutex.withLock {
                    val existing = id?.let { repository.get(it) }
                    // Opening/recreating an unchanged note must not change its timestamp or ordering.
                    if (existing != null && existing.title == title && existing.body == body) {
                        if (version == saveVersion) {
                            onSaved(existing)
                            onComplete()
                        }
                        return@withLock
                    }
                    // Clearing an existing note is an edit; an untouched new draft is not a note.
                    if (title.isBlank() && body.isBlank() && existing == null) {
                        onComplete()
                        return@withLock
                    }
                    val saved = repository.save(id, title, body)
                    if (version == saveVersion) {
                        onSaved(saved)
                        onComplete()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (version == saveVersion) failedSave.value = true
            }
        }
    }

    fun save(id: String?, title: String, body: String, onSaved: (NoteEntity) -> Unit = {}) {
        saveImmediately(id, title, body, onSaved)
    }

    fun togglePinned(note: NoteEntity) {
        viewModelScope.launch { repository.setPinned(note.id, !note.isPinned) }
    }

    fun archive(note: NoteEntity) {
        viewModelScope.launch { repository.setArchived(note.id, true) }
    }

    fun trash(note: NoteEntity) {
        viewModelScope.launch { repository.setTrashed(note.id, true) }
    }

    fun restore(note: NoteEntity) {
        viewModelScope.launch {
            if (note.isTrashed) repository.setTrashed(note.id, false)
            else repository.setArchived(note.id, false)
        }
    }

    override fun onCleared() {
        draftSaveJob?.cancel()
        super.onCleared()
    }
}
