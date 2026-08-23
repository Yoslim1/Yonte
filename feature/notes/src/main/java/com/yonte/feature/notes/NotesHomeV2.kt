package com.yonte.feature.notes

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yonte.core.database.NoteEntity
import kotlinx.coroutines.launch

private enum class NotesViewMode { LIST, GRID }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesHomeV2(
    notes: List<NoteEntity>,
    onSearch: (String) -> Unit,
    onNew: () -> Unit,
    onEdit: (NoteEntity) -> Unit,
    onPin: (NoteEntity) -> Unit,
    onArchive: (NoteEntity) -> Unit,
    onTrash: (NoteEntity) -> Unit,
    onSettings: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var viewMode by rememberSaveable { mutableStateOf(NotesViewMode.LIST) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val isArabic = LocalLayoutDirection.current == LayoutDirection.Rtl
    val appSubtitle = if (isArabic) "مساحتك الشخصية" else "Your personal space"
    val searchLabel = if (isArabic) "ابحث في ملاحظاتك" else "Search your notes"
    val listLabel = if (isArabic) "قائمة" else "List"
    val gridLabel = if (isArabic) "شبكة" else "Grid"
    val pinnedLabel = if (isArabic) "مثبتة" else "Pinned"
    val recentLabel = if (isArabic) "الأحدث" else "Recent"
    val openMenuLabel = if (isArabic) "فتح القائمة" else "Open menu"
    val settingsLabel = if (isArabic) "الإعدادات" else "Settings"
    val tags = notes.flatMap { notePreview(it.title, it.body).tags }.distinct().take(12)
    val filtered = notes.filter { note ->
        selectedTag == null || notePreview(note.title, note.body).tags.contains(selectedTag)
    }
    val pinned = filtered.filter { it.isPinned }
    val recent = filtered.filterNot { it.isPinned }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Yonte", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(24.dp))
                NavigationDrawerItem(
                    label = { Text(if (isArabic) "ملاحظاتي" else "My notes") },
                    selected = true,
                    onClick = { drawerScope.launch { drawerState.close() } },
                )
                NavigationDrawerItem(
                    label = { Text(settingsLabel) },
                    selected = false,
                    onClick = { drawerScope.launch { drawerState.close() }; onSettings() },
                )
            }
        },
    ) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Yonte", fontWeight = FontWeight.Bold)
                        Text(appSubtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { drawerScope.launch { drawerState.open() } }) {
                        Icon(Icons.Outlined.Menu, contentDescription = openMenuLabel)
                    }
                },
                actions = {},
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNew, containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Text("+", fontSize = 28.sp, fontWeight = FontWeight.Light)
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; onSearch(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text(searchLabel) },
                shape = RoundedCornerShape(18.dp),
            )
            if (tags.isNotEmpty()) {
                LazyColumnChips(tags, selectedTag) { selectedTag = if (selectedTag == it) null else it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                FilterChip(
                    selected = viewMode == NotesViewMode.LIST,
                    onClick = { viewMode = NotesViewMode.LIST },
                    label = { Text(listLabel) },
                    leadingIcon = { Icon(Icons.Outlined.ViewList, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
                FilterChip(
                    selected = viewMode == NotesViewMode.GRID,
                    onClick = { viewMode = NotesViewMode.GRID },
                    label = { Text(gridLabel) },
                    leadingIcon = { Icon(Icons.Outlined.GridView, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
            }
            Spacer(Modifier.height(12.dp))
            if (filtered.isEmpty()) {
                EmptyWorkspace(onNew)
            } else if (viewMode == NotesViewMode.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (pinned.isNotEmpty()) item { SectionLabel(pinnedLabel) }
                    items(pinned, key = { "p-${it.id}" }) { note -> NoteCardV2(note, onEdit, onPin, onArchive, onTrash) }
                    if (recent.isNotEmpty()) item { SectionLabel(recentLabel) }
                    items(recent, key = { "r-${it.id}" }) { note -> NoteCardV2(note, onEdit, onPin, onArchive, onTrash) }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (pinned.isNotEmpty()) {
                        item { SectionLabel(pinnedLabel) }
                        items(pinned, key = { "p-${it.id}" }) { note -> NoteCardV2(note, onEdit, onPin, onArchive, onTrash) }
                    }
                    if (recent.isNotEmpty()) {
                        item { SectionLabel(recentLabel) }
                        items(recent, key = { "r-${it.id}" }) { note -> NoteCardV2(note, onEdit, onPin, onArchive, onTrash) }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun LazyColumnChips(tags: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tags.forEach { tag ->
            AssistChip(onClick = { onSelect(tag) }, label = { Text("#$tag") }, leadingIcon = if (tag == selected) ({ Icon(Icons.Outlined.PushPin, null, Modifier.size(14.dp)) }) else null)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
}

@Composable
private fun EmptyWorkspace(onNew: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("مساحتك جاهزة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("اكتب فكرة، قائمة، أو شيئاً تريد تذكره", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onNew) { Text("أنشئ أول ملاحظة") }
        }
    }
}

@Composable
private fun NoteCardV2(
    note: NoteEntity,
    onEdit: (NoteEntity) -> Unit,
    onPin: (NoteEntity) -> Unit,
    onArchive: (NoteEntity) -> Unit,
    onTrash: (NoteEntity) -> Unit,
) {
    val preview = notePreview(note.title, note.body)
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit(note) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(preview.title.ifBlank { "بدون عنوان" }, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (note.isPinned) Icon(Icons.Outlined.PushPin, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(preview.body.ifBlank { "ملاحظة فارغة" }, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
            if (preview.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(preview.tags.joinToString("  ") { "#$it" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(DateUtils.getRelativeTimeSpanString(note.updatedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                IconButton(onClick = { onPin(note) }) { Icon(Icons.Outlined.PushPin, contentDescription = "تثبيت", modifier = Modifier.size(18.dp)) }
                IconButton(onClick = { onArchive(note) }) { Icon(Icons.Outlined.Archive, contentDescription = "أرشفة", modifier = Modifier.size(18.dp)) }
                IconButton(onClick = { onTrash(note) }) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "حذف", modifier = Modifier.size(18.dp)) }
            }
        }
    }
}
