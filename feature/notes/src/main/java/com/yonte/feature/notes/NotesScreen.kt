package com.yonte.feature.notes

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
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
import java.util.UUID
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.yonte.core.backup.BackupGateway
import com.yonte.core.backup.BackupNote
import com.yonte.core.database.ArabicNormalizer
import com.yonte.core.database.NoteEntity
import com.yonte.core.database.NoteRepository
import com.yonte.core.update.UpdateGateway
import com.yonte.core.update.UpdateInfo
import kotlinx.coroutines.launch
import com.yonte.core.navigation.NotesNavigator

@Composable
fun NotesRoute(
    repository: NoteRepository,
    sharedText: String? = null,
    onSharedTextConsumed: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val vm: NotesViewModel = hiltViewModel()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editorNote by remember { mutableStateOf<NoteEntity?>(null) }
    var editorInitialText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            editorNote = null
            editorInitialText = sharedText
            onSharedTextConsumed()
        }
    }

    val isArabicDevice = Locale.getDefault().language == "ar"
    CompositionLocalProvider(LocalLayoutDirection provides if (isArabicDevice) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        if (editorNote != null || editorInitialText != null) {
        NoteEditor(
            note = editorNote,
            initialText = editorInitialText,
            onLeave = { id, title, body ->
                vm.saveImmediately(id, title, body)
                editorNote = null
                editorInitialText = null
            },
            onAutoSave = { id, title, body, onSaved -> vm.autosave(id, title, body, onSaved) },
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
                onSettings = onOpenSettings,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditor(
    note: NoteEntity?,
    initialText: String?,
    onLeave: (String?, String, String) -> Unit,
    onAutoSave: (String?, String, String, (NoteEntity) -> Unit) -> Unit,
) {
    val isArabic = LocalLayoutDirection.current == LayoutDirection.Rtl
    val labels = remember(isArabic) { Labels.arabic.takeIf { isArabic } ?: Labels.english }
    var draftId by remember(note?.id, initialText) { mutableStateOf(note?.id ?: UUID.randomUUID().toString()) }
    var title by remember(note?.id, initialText) { mutableStateOf(note?.title.orEmpty()) }
    var body by remember(note?.id, initialText) { mutableStateOf(note?.body ?: initialText.orEmpty()) }
    val latestDraftId by rememberUpdatedState(draftId)

    val latestTitle by rememberUpdatedState(title)
    val latestBody by rememberUpdatedState(body)
    var hasLeft by remember(note?.id, initialText) { mutableStateOf(false) }
    val latestHasLeft by rememberUpdatedState(hasLeft)
    fun leave() {
        if (!hasLeft) {
            hasLeft = true
            onLeave(draftId, title, body)
        }
    }
    BackHandler(enabled = true, onBack = ::leave)
    DisposableEffect(note?.id, initialText) {
        onDispose { if (!latestHasLeft) onLeave(latestDraftId, latestTitle, latestBody) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (note == null) labels.newNote else labels.editNote) },
                navigationIcon = { TextButton(onClick = ::leave) { Text(labels.cancel) } },
                actions = { TextButton(onClick = ::leave) { Text(labels.save, fontWeight = FontWeight.Bold) } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it; onAutoSave(draftId, title, body) { draftId = it.id } }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(labels.title) }, textStyle = MaterialTheme.typography.titleLarge)
            EditorToolbar(isArabic = isArabic) { prefix ->
                body = appendEditorAction(body, prefix)
                onAutoSave(draftId, title, body) { draftId = it.id }
            }
            OutlinedTextField(value = body, onValueChange = { body = it; onAutoSave(draftId, title, body) { draftId = it.id } }, modifier = Modifier.fillMaxWidth().weight(1f), placeholder = { Text(labels.writeHere) }, minLines = 10)
            Text(labels.autosaveHint, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
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
