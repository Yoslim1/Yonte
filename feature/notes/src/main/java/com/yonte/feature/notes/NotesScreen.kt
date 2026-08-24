package com.yonte.feature.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.util.Locale
import java.util.UUID
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.yonte.core.database.NoteEntity
import com.yonte.core.database.NoteRepository

@Composable
fun NotesRoute(
    repository: NoteRepository,
    sharedText: String? = null,
    onSharedTextConsumed: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val vm: NotesViewModel = hiltViewModel()
    val notes by vm.notes.collectAsStateWithLifecycle()
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
    var isSaved by rememberSaveable(note?.id, initialText) { mutableStateOf(true) }
    val latestDraftId by rememberUpdatedState(draftId)
    val latestTitle by rememberUpdatedState(title)
    val latestBody by rememberUpdatedState(body)
    var hasLeft by remember(note?.id, initialText) { mutableStateOf(false) }
    val latestHasLeft by rememberUpdatedState(hasLeft)

    fun saveDraft() {
        isSaved = false
        onAutoSave(draftId, title, body) {
            draftId = it.id
            isSaved = true
        }
    }
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (note == null) labels.newNote else labels.editNote, style = MaterialTheme.typography.titleMedium)
                        Text(if (isSaved) (if (isArabic) "محفوظ محلياً" else "Saved locally") else (if (isArabic) "جارٍ الحفظ…" else "Saving…"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = ::leave) { Icon(Icons.Outlined.Close, contentDescription = labels.cancel) } },
                actions = { TextButton(onClick = ::leave) { Text(if (isArabic) "تم" else "Done", fontWeight = FontWeight.Bold) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(padding).padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(if (isArabic) "مساحة لبدء فكرة" else "A space to begin an idea", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            BasicTextField(
                value = title,
                onValueChange = { title = it; saveDraft() },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.displaySmall.copy(color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (title.isBlank()) Text(labels.title, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
                    inner()
                },
            )
            EditorToolbar(isArabic = isArabic) { prefix ->
                body = appendEditorAction(body, prefix)
                saveDraft()
            }
            BasicTextField(
                value = body,
                onValueChange = { body = it; saveDraft() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 420.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (body.isBlank()) Text(labels.writeHere, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    inner()
                },
            )
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
