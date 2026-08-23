package com.yonte.feature.notes

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yonte.core.backup.BackupNote
import com.yonte.core.backup.BackupService
import com.yonte.core.database.ArabicNormalizer
import com.yonte.core.database.NoteEntity
import com.yonte.core.database.NoteRepository
import com.yonte.core.security.EncryptionManager
import kotlinx.coroutines.launch
import com.yonte.core.navigation.NotesNavigator

@Composable
fun NotesRoute(
    repository: NoteRepository,
    sharedText: String? = null,
    onSharedTextConsumed: () -> Unit = {},
    darkTheme: Boolean = false,
    onThemeChanged: (Boolean) -> Unit = {},
) {
    val vm: NotesViewModel = viewModel(factory = NotesViewModel.factory(repository))
    val notes by vm.notes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupService = remember { BackupService(EncryptionManager()) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                backupService.exportNotes(context.contentResolver, uri, repository.getAll().map { note ->
                    BackupNote(note.id, note.title, note.body, note.isPinned, note.createdAt, note.updatedAt)
                })
            }.onSuccess { Toast.makeText(context, "Backup exported", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "Backup export failed", Toast.LENGTH_SHORT).show() }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                backupService.importNotes(context.contentResolver, uri).map { item ->
                    NoteEntity(item.id, item.title, item.body, ArabicNormalizer.normalize("${item.title} ${item.body}"), item.isPinned, false, false, item.createdAt, item.updatedAt)
                }
            }.onSuccess { restored ->
                repository.restore(restored)
                Toast.makeText(context, "Backup imported", Toast.LENGTH_SHORT).show()
            }.onFailure { Toast.makeText(context, "Backup import failed", Toast.LENGTH_SHORT).show() }
        }
    }
    var editorNote by remember { mutableStateOf<NoteEntity?>(null) }
    var editorInitialText by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            editorNote = null
            editorInitialText = sharedText
            onSharedTextConsumed()
        }
    }

    val isArabicDevice = Locale.getDefault().language == "ar"
    CompositionLocalProvider(LocalLayoutDirection provides if (isArabicDevice) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        if (showSettings) {
            SettingsDialog(
                darkTheme = darkTheme,
                onThemeChanged = onThemeChanged,
                onExport = { exportLauncher.launch("yonte-backup.ynt") },
                onImport = { importLauncher.launch(arrayOf("application/octet-stream", "application/json", "*/*")) },
                onDismiss = { showSettings = false },
            )
        } else if (editorNote != null || editorInitialText != null) {
        NoteEditor(
            note = editorNote,
            initialText = editorInitialText,
            onBack = {
                editorNote = null
                editorInitialText = null
            },
            onSave = { id, title, body ->
                vm.save(id, title, body) {
                    editorNote = null
                    editorInitialText = null
                }
            },
        )
    } else {
            NotesHomeV2(
                notes = notes,
            onSearch = vm::search,
            onNew = { editorNote = null; editorInitialText = "" },
            onEdit = { editorNote = it },
            onPin = vm::togglePinned,
            onArchive = vm::archive,
            onTrash = vm::trash,
                onSettings = { showSettings = true },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesHome(
    notes: List<NoteEntity>,
    onSearch: (String) -> Unit,
    onNew: () -> Unit,
    onEdit: (NoteEntity) -> Unit,
    onPin: (NoteEntity) -> Unit,
    onArchive: (NoteEntity) -> Unit,
    onTrash: (NoteEntity) -> Unit,
    onSettings: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val isArabic = LocalLayoutDirection.current == LayoutDirection.Rtl
    val labels = remember(isArabic) { Labels.arabic.takeIf { isArabic } ?: Labels.english }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(labels.appName, fontWeight = FontWeight.Bold)
                        Text(labels.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = labels.settings) }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNew, containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Outlined.EditNote, contentDescription = labels.newNote)
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
                placeholder = { Text(labels.search) },
                shape = RoundedCornerShape(18.dp),
            )
            Spacer(Modifier.height(16.dp))
            if (notes.isEmpty()) {
                EmptyNotes(labels = labels, onNew = onNew)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(note, labels, onEdit, onPin, onArchive, onTrash)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNotes(labels: Labels, onNew: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.padding(18.dp).size(44.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(Modifier.height(16.dp))
            Text(labels.emptyTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(labels.emptyBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onNew) { Text(labels.createFirst) }
        }
    }
}

@Composable
private fun NoteCard(
    note: NoteEntity,
    labels: Labels,
    onEdit: (NoteEntity) -> Unit,
    onPin: (NoteEntity) -> Unit,
    onArchive: (NoteEntity) -> Unit,
    onTrash: (NoteEntity) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit(note) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(note.title.ifBlank { labels.untitled }, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (note.isPinned) Icon(Icons.Outlined.PushPin, contentDescription = labels.unpin, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
            if (note.body.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(note.body, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { onPin(note) }) { Icon(if (note.isPinned) Icons.Outlined.Star else Icons.Outlined.PushPin, contentDescription = labels.pin) }
                IconButton(onClick = { onArchive(note) }) { Icon(Icons.Outlined.Archive, contentDescription = labels.archive) }
                IconButton(onClick = { onTrash(note) }) { Icon(Icons.Outlined.DeleteOutline, contentDescription = labels.delete) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditor(
    note: NoteEntity?,
    initialText: String?,
    onBack: () -> Unit,
    onSave: (String?, String, String) -> Unit,
) {
    val isArabic = LocalLayoutDirection.current == LayoutDirection.Rtl
    val labels = remember(isArabic) { Labels.arabic.takeIf { isArabic } ?: Labels.english }
    var title by remember(note?.id, initialText) { mutableStateOf(note?.title.orEmpty()) }
    var body by remember(note?.id, initialText) { mutableStateOf(note?.body ?: initialText.orEmpty()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (note == null) labels.newNote else labels.editNote) },
                navigationIcon = { TextButton(onClick = onBack) { Text(labels.cancel) } },
                actions = { TextButton(onClick = { onSave(note?.id, title, body) }) { Text(labels.save, fontWeight = FontWeight.Bold) } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(labels.title) }, textStyle = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = body, onValueChange = { body = it }, modifier = Modifier.fillMaxWidth().weight(1f), placeholder = { Text(labels.writeHere) }, minLines = 10)
            Text(labels.autosaveHint, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsDialog(
    darkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isArabic = LocalLayoutDirection.current == LayoutDirection.Rtl
    val labels = remember(isArabic) { Labels.arabic.takeIf { isArabic } ?: Labels.english }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(labels.settings) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (isArabic) "الوضع الداكن" else "Dark mode")
                    Switch(checked = darkTheme, onCheckedChange = onThemeChanged)
                }
                Divider()
                Text(if (isArabic) "البيانات محلية على جهازك" else "Your data stays on this device", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text(if (isArabic) "تصدير نسخة" else "Export") }
                    TextButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text(if (isArabic) "استيراد نسخة" else "Import") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(if (isArabic) "تم" else "Done") } },
    )
}

private data class Labels(
    val appName: String,
    val subtitle: String,
    val settings: String,
    val search: String,
    val newNote: String,
    val editNote: String,
    val title: String,
    val writeHere: String,
    val save: String,
    val cancel: String,
    val autosaveHint: String,
    val emptyTitle: String,
    val emptyBody: String,
    val createFirst: String,
    val untitled: String,
    val pin: String,
    val unpin: String,
    val archive: String,
    val delete: String,
) {
    companion object {
        val arabic = Labels("Yonte", "مساحتك الشخصية", "الإعدادات", "ابحث في ملاحظاتك", "ملاحظة جديدة", "تعديل الملاحظة", "العنوان", "اكتب ملاحظتك هنا…", "حفظ", "إلغاء", "يحفظ Yonte بياناتك محلياً على جهازك", "لا توجد ملاحظات بعد", "ابدأ بفكرة صغيرة واحفظها هنا", "أنشئ أول ملاحظة", "بدون عنوان", "تثبيت", "إلغاء التثبيت", "أرشفة", "حذف")
        val english = Labels("Yonte", "Your personal space", "Settings", "Search your notes", "New note", "Edit note", "Title", "Write your note here…", "Save", "Cancel", "Yonte saves your data locally on this device", "No notes yet", "Start with a small idea and save it here", "Create your first note", "Untitled", "Pin", "Unpin", "Archive", "Delete")
    }
}
