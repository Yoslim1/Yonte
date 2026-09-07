package com.yonte.feature.notes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.yonte.core.designsystem.YonteMark
import com.yonte.core.database.NoteEntity

internal enum class NotesViewMode { LIST, GRID }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesHomeScreen(
    notes: List<NoteEntity>,
    onSearch: (String) -> Unit,
    onNew: (String) -> Unit,
    onEdit: (NoteEntity) -> Unit,
    onPin: (NoteEntity) -> Unit,
    onArchive: (NoteEntity) -> Unit,
    onTrash: (NoteEntity) -> Unit,
    onRestore: (NoteEntity) -> Unit,
    collection: NotesCollection,
    onCollectionChange: (NotesCollection) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var viewMode by rememberSaveable { mutableStateOf(NotesViewMode.LIST) }
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }
    val isArabic = LocalLayoutDirection.current == LayoutDirection.Rtl
    val pageTitle = if (isArabic) "ملاحظاتك" else "Your notes"
    val pageSubtitle = if (isArabic) "أفكارك اليوم. أثرك غداً." else "Ideas today. A legacy tomorrow."
    val searchLabel = if (isArabic) "ابحث في ملاحظاتك" else "Search your notes"
    LaunchedEffect(query) { onSearch(query) }
    val tags = notes.flatMap { notePreview(it.title, it.body).tags }.distinct().take(10)
    val filtered = notes.filter { selectedTag == null || notePreview(it.title, it.body).tags.contains(selectedTag) }
    val pinned = filtered.filter { it.isPinned }
    val recent = filtered.filterNot { it.isPinned }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showQuickAdd = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(if (isArabic) "ملاحظة جديدة" else "New note") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = if (viewMode == NotesViewMode.GRID) GridCells.Adaptive(minSize = 170.dp) else GridCells.Fixed(1),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 112.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            Column(
                Modifier.padding(vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Menu, contentDescription = if (isArabic) "فتح الإعدادات" else "Open settings")
                    }
                    Text("Yonte", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    YonteMark()
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(pageTitle, style = MaterialTheme.typography.headlineMedium)
                    Text(pageSubtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                NotesSearchField(query, searchLabel) { query = it }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NotesCollection.entries.forEach { item ->
                        FilterChip(
                            selected = collection == item,
                            onClick = { selectedTag = null; onCollectionChange(item) },
                            label = { Text(item.label(isArabic)) },
                        )
                    }
                }
                if (tags.isNotEmpty()) NotesTagStrip(tags, selectedTag) { selectedTag = if (selectedTag == it) null else it }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(collection.label(isArabic), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    NotesViewModeToggle(viewMode, isArabic) { viewMode = it }
                }
            }
            }
            if (filtered.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    NotesEmptyState(
                        onNew = onNew,
                        isArabic = isArabic,
                        collection = collection,
                        hasFilters = query.isNotBlank() || selectedTag != null,
                        onClearFilters = { query = ""; selectedTag = null },
                    )
                }
            } else {
                if (pinned.isNotEmpty()) item(key = "pinned-header", span = { GridItemSpan(maxLineSpan) }) {
                    NotesSectionHeader(if (isArabic) "المثبتة" else "Pinned", true)
                }
                items(pinned, key = { "p-${it.id}" }) { note -> NoteCard(note, isArabic, onEdit, onPin, onArchive, onTrash, onRestore) }
                if (recent.isNotEmpty()) item(key = "recent-header", span = { GridItemSpan(maxLineSpan) }) {
                    NotesSectionHeader(if (isArabic) "الأحدث" else "Recent", true)
                }
                items(recent, key = { "r-${it.id}" }) { note -> NoteCard(note, isArabic, onEdit, onPin, onArchive, onTrash, onRestore) }
            }
        }
    }
    if (showQuickAdd) {
        ModalBottomSheet(
            onDismissRequest = { showQuickAdd = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text(if (isArabic) "إضافة جديدة" else "Create new", style = MaterialTheme.typography.headlineSmall)
                Text(if (isArabic) "ابدأ من الفكرة، وسيتكفل Yonte بالباقي" else "Start with the idea; Yonte handles the rest", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                QuickAddChoice(
                    title = if (isArabic) "ملاحظة" else "Note",
                    subtitle = if (isArabic) "مساحة مفتوحة للكتابة" else "An open space for writing",
                    icon = Icons.Outlined.NoteAdd,
                    onClick = { showQuickAdd = false; onNew("") },
                )
                QuickAddChoice(
                    title = if (isArabic) "مهمة" else "Task",
                    subtitle = if (isArabic) "سطر قابل للإنجاز داخل ملاحظتك" else "A checkable line inside your note",
                    icon = Icons.Outlined.CheckCircle,
                    onClick = { showQuickAdd = false; onNew("- [ ] ") },
                )
                Spacer(Modifier.height(22.dp))
            }
        }
    }
}

private fun NotesCollection.label(isArabic: Boolean): String = when (this) {
    NotesCollection.ACTIVE -> if (isArabic) "الملاحظات" else "Notes"
    NotesCollection.ARCHIVED -> if (isArabic) "الأرشيف" else "Archive"
    NotesCollection.TRASHED -> if (isArabic) "المحذوفات" else "Trash"
}
