package com.yonte.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.yonte.core.database.NoteEntity
import kotlinx.coroutines.launch

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
    onSettings: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var viewMode by rememberSaveable { mutableStateOf(NotesViewMode.LIST) }
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val isArabic = LocalLayoutDirection.current == LayoutDirection.Rtl
    val pageTitle = if (isArabic) "ملاحظاتك" else "Your notes"
    val pageSubtitle = if (isArabic) "مساحة هادئة لأفكارك اليومية" else "A calm space for everyday thoughts"
    val searchLabel = if (isArabic) "ابحث في ملاحظاتك" else "Search your notes"
    val tags = notes.flatMap { notePreview(it.title, it.body).tags }.distinct().take(10)
    val filtered = notes.filter { selectedTag == null || notePreview(it.title, it.body).tags.contains(selectedTag) }
    val pinned = filtered.filter { it.isPinned }
    val recent = filtered.filterNot { it.isPinned }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                modifier = Modifier.fillMaxWidth(0.86f),
            ) {
                Column(Modifier.fillMaxSize().padding(20.dp)) {
                    Text("Yonte", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(if (isArabic) "مساحتك الخاصة" else "Your private space", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(28.dp))
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                        Column(Modifier.padding(16.dp)) {
                            Text(if (isArabic) "ملاحظاتك محلية" else "Your notes are local", fontWeight = FontWeight.SemiBold)
                            Text(if (isArabic) "لا حسابات ولا تتبع" else "No accounts. No tracking.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    NavigationDrawerItem(
                        label = { Text(if (isArabic) "ملاحظاتي" else "My notes") },
                        icon = { Icon(Icons.Outlined.List, contentDescription = null) },
                        selected = true,
                        onClick = { drawerScope.launch { drawerState.close() } },
                    )
                    NavigationDrawerItem(
                        label = { Text(if (isArabic) "الإعدادات" else "Settings") },
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        selected = false,
                        onClick = { drawerScope.launch { drawerState.close() }; onSettings() },
                    )
                    Spacer(Modifier.weight(1f))
                    Text("Yonte", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (isArabic) "محلي بالكامل" else "Fully local", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
    ) {
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
            Column(Modifier.fillMaxSize().padding(padding)) {
                Column(
                    Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(pageTitle, style = MaterialTheme.typography.headlineSmall)
                            Text(pageSubtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { drawerScope.launch { drawerState.open() } }) {
                            Icon(Icons.Outlined.Menu, contentDescription = if (isArabic) "فتح القائمة" else "Open menu")
                        }
                    }
                    NotesSearchField(query, searchLabel) { query = it; onSearch(it) }
                    if (tags.isNotEmpty()) NotesTagStrip(tags, selectedTag) { selectedTag = if (selectedTag == it) null else it }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isArabic) "ملاحظاتك" else "Notes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        NotesViewModeToggle(viewMode, isArabic) { viewMode = it }
                    }
                }
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { NotesEmptyState(onNew, isArabic) }
                } else if (viewMode == NotesViewMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 170.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 112.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (pinned.isNotEmpty()) item { NotesSectionHeader(if (isArabic) "المثبتة" else "Pinned", true) }
                        items(pinned, key = { "p-${it.id}" }) { note -> NoteCard(note, isArabic, onEdit, onPin, onArchive, onTrash) }
                        if (recent.isNotEmpty()) item { NotesSectionHeader(if (isArabic) "الأحدث" else "Recent", true) }
                        items(recent, key = { "r-${it.id}" }) { note -> NoteCard(note, isArabic, onEdit, onPin, onArchive, onTrash) }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (pinned.isNotEmpty()) {
                            item { NotesSectionHeader(if (isArabic) "المثبتة" else "Pinned", true) }
                            items(pinned, key = { "p-${it.id}" }) { note -> NoteCard(note, isArabic, onEdit, onPin, onArchive, onTrash) }
                        }
                        if (recent.isNotEmpty()) {
                            item { NotesSectionHeader(if (isArabic) "الأحدث" else "Recent", true) }
                            items(recent, key = { "r-${it.id}" }) { note -> NoteCard(note, isArabic, onEdit, onPin, onArchive, onTrash) }
                        }
                    }
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
}
