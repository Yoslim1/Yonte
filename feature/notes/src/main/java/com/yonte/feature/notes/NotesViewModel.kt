package com.yonte.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yonte.core.database.NoteEntity
import com.yonte.core.database.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    val notes: StateFlow<List<NoteEntity>> = query
        .debounce(140)
        .flatMapLatest { value ->
            if (value.isBlank()) repository.observeActive()
            else flow { emit(repository.search(value)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun search(value: String) { query.value = value }

    fun save(id: String?, title: String, body: String, onSaved: (NoteEntity) -> Unit = {}) {
        viewModelScope.launch {
            onSaved(repository.save(id, title, body))
        }
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
        viewModelScope.launch { repository.setTrashed(note.id, false) }
    }
}
