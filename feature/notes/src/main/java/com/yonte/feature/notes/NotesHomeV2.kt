package com.yonte.feature.notes

import android.text.format.DateUtils
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
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
                    onClick = onNew,
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
                    SearchField(query, searchLabel) { query = it; onSearch(it) }
                    if (tags.isNotEmpty()) TagStrip(tags, selectedTag) { selectedTag = if (selectedTag == it) null else it }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isArabic) "ملاحظاتك" else "Notes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        ViewModeToggle(viewMode, isArabic) { viewMode = it }
                    }
                }
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyWorkspace(onNew, isArabic) }
                } else if (viewMode == NotesViewMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 170.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 112.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (pinned.isNotEmpty()) item { SectionHeader(if (isArabic) "المثبتة" else "Pinned", true) }
                        items(pinned, key = { "p-${it.id}" }) { note -> NoteCardV2(note, isArabic, onEdit, onPin, onArchive, onTrash) }
                        if (recent.isNotEmpty()) item { SectionHeader(if (isArabic) "الأحدث" else "Recent", true) }
                        items(recent, key = { "r-${it.id}" }) { note -> NoteCardV2(note, isArabic, onEdit, onPin, onArchive, onTrash) }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (pinned.isNotEmpty()) {
                            item { SectionHeader(if (isArabic) "المثبتة" else "Pinned", true) }
                            items(pinned, key = { "p-${it.id}" }) { note -> NoteCardV2(note, isArabic, onEdit, onPin, onArchive, onTrash) }
                        }
                        if (recent.isNotEmpty()) {
                            item { SectionHeader(if (isArabic) "الأحدث" else "Recent", true) }
                            items(recent, key = { "r-${it.id}" }) { note -> NoteCardV2(note, isArabic, onEdit, onPin, onArchive, onTrash) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, label: String, onValueChange: (String) -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (value.isBlank()) Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                },
            )
        }
    }
}

@Composable
private fun TagStrip(tags: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { tag ->
            FilterChip(selected = tag == selected, onClick = { onSelect(tag) }, label = { Text("#$tag") })
        }
    }
}

@Composable
private fun ViewModeToggle(mode: NotesViewMode, isArabic: Boolean, onModeChange: (NotesViewMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(
            selected = mode == NotesViewMode.LIST,
            onClick = { onModeChange(NotesViewMode.LIST) },
            label = { Text(if (isArabic) "قائمة" else "List") },
            leadingIcon = { Icon(Icons.Outlined.ViewList, contentDescription = null, modifier = Modifier.size(16.dp)) },
        )
        FilterChip(
            selected = mode == NotesViewMode.GRID,
            onClick = { onModeChange(NotesViewMode.GRID) },
            label = { Text(if (isArabic) "شبكة" else "Grid") },
            leadingIcon = { Icon(Icons.Outlined.GridView, contentDescription = null, modifier = Modifier.size(16.dp)) },
        )
    }
}

@Composable
private fun SectionHeader(text: String, visible: Boolean) {
    if (visible) Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun EmptyWorkspace(onNew: () -> Unit, isArabic: Boolean) {
    Box(Modifier.fillMaxWidth().padding(vertical = 72.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (isArabic) "مساحة جديدة" else "A fresh space", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(if (isArabic) "ابدأ بفكرة صغيرة، وسيحفظها Yonte تلقائياً" else "Start small; Yonte saves as you go", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onNew) { Text(if (isArabic) "أنشئ أول ملاحظة" else "Create your first note") }
        }
    }
}

@Composable
private fun NoteCardV2(
    note: NoteEntity,
    isArabic: Boolean,
    onEdit: (NoteEntity) -> Unit,
    onPin: (NoteEntity) -> Unit,
    onArchive: (NoteEntity) -> Unit,
    onTrash: (NoteEntity) -> Unit,
) {
    val preview = notePreview(note.title, note.body)
    var menuExpanded by androidx.compose.runtime.remember { mutableStateOf(false) }
    Card(
        onClick = { onEdit(note) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (note.isPinned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f) else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(preview.title.ifBlank { if (isArabic) "بدون عنوان" else "Untitled" }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (note.isPinned) Icon(Icons.Outlined.PushPin, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = if (isArabic) "المزيد" else "More", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text(if (note.isPinned) (if (isArabic) "إلغاء التثبيت" else "Unpin") else (if (isArabic) "تثبيت" else "Pin")) }, onClick = { menuExpanded = false; onPin(note) })
                        DropdownMenuItem(text = { Text(if (isArabic) "أرشفة" else "Archive") }, onClick = { menuExpanded = false; onArchive(note) })
                        DropdownMenuItem(text = { Text(if (isArabic) "حذف" else "Delete") }, onClick = { menuExpanded = false; onTrash(note) })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(preview.body.ifBlank { if (isArabic) "ملاحظة فارغة" else "Empty note" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (preview.tags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(preview.tags.joinToString("  ") { "#$it" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(DateUtils.getRelativeTimeSpanString(note.updatedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            }
        }
    }
}
