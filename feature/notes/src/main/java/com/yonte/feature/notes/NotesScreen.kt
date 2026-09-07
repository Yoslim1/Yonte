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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.yonte.core.database.NoteRepository

@Composable
fun NotesRoute(
    repository: NoteRepository,
    sharedText: String? = null,
    onSharedTextConsumed: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onEditingChanged: (Boolean) -> Unit = {},
) {
    val vm: NotesViewModel = hiltViewModel()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val editor by vm.editor.collectAsStateWithLifecycle()
    LaunchedEffect(editor != null) { onEditingChanged(editor != null) }
    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            vm.openEditor(text = sharedText)
            onSharedTextConsumed()
        }
    }

    val collection by vm.collection.collectAsStateWithLifecycle()
    val saveFailed by vm.saveFailed.collectAsStateWithLifecycle()
    run {
        if (editor != null) {
        NoteEditor(
            session = requireNotNull(editor),
            onDraftChanged = vm::updateEditor,
            saveFailed = saveFailed,
            onLeave = { id, title, body ->
                vm.saveImmediately(id, title, body, onComplete = {
                    vm.closeEditor(id)
                })
            },
            onAutoSave = { id, title, body, onSaved -> vm.autosave(id, title, body, onComplete = onSaved) },
        )
    } else {
            NotesHomeScreen(
                notes = notes,
            onSearch = vm::search,
            onNew = { initialText -> vm.openEditor(text = initialText) },
            onEdit = { vm.openEditor(note = it) },
            onPin = vm::togglePinned,
            onArchive = vm::archive,
            onTrash = vm::trash,
                onOpenSettings = onOpenSettings,
                collection = collection,
                onCollectionChange = vm::selectCollection,
                onRestore = vm::restore,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditor(
    session: EditorSession,
    onDraftChanged: (String, String) -> Unit,
    saveFailed: Boolean,
    onLeave: (String?, String, String) -> Unit,
    onAutoSave: (String?, String, String, () -> Unit) -> Unit,
) {
    val isArabic = LocalLayoutDirection.current == LayoutDirection.Rtl
    val labels = remember(isArabic) { Labels.arabic.takeIf { isArabic } ?: Labels.english }
    val note = session.note
    val draftId = session.id
    var title by remember(session.id) { mutableStateOf(session.title) }
    var body by remember(session.id) { mutableStateOf(TextFieldValue(session.body)) }
    var isSaved by remember(session.id) { mutableStateOf(note != null || session.body.isBlank()) }
    var hasLeft by remember(session.id) { mutableStateOf(false) }
    LaunchedEffect(saveFailed) { if (saveFailed) hasLeft = false }

    var revision by remember { mutableStateOf(0) }
    fun saveDraft() {
        if (hasLeft) return
        onDraftChanged(title, body.text)
        revision += 1
        val savingRevision = revision
        isSaved = false
        onAutoSave(draftId, title, body.text) {
            if (savingRevision == revision) isSaved = true
        }
    }
    fun leave() {
        if (!hasLeft) {
            hasLeft = true
            onLeave(draftId, title, body.text)
        }
    }
    BackHandler(enabled = true, onBack = ::leave)
    LaunchedEffect(session.id) {
        // Re-acknowledge the in-memory draft after recreation; no plaintext Bundle state.
        saveDraft()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (note == null) labels.newNote else labels.editNote, style = MaterialTheme.typography.titleMedium)
                        Text(if (saveFailed) (if (isArabic) "تعذّر الحفظ — حاول مجددًا" else "Save failed — try again") else if (isSaved) (if (isArabic) "محفوظ محلياً" else "Saved locally") else (if (isArabic) "جارٍ الحفظ…" else "Saving…"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = ::leave, enabled = !hasLeft) { Icon(Icons.Outlined.Close, contentDescription = if (isArabic) "العودة إلى الملاحظات" else "Back to notes") } },
                actions = { TextButton(onClick = ::leave, enabled = !hasLeft) { Text(if (isArabic) "تم" else "Done", fontWeight = FontWeight.Bold) } },
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
                onValueChange = { if (!hasLeft) { title = it; saveDraft() } },
                readOnly = hasLeft,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.displaySmall.copy(color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (title.isBlank()) Text(labels.title, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
                    inner()
                },
            )
            EditorToolbar(isArabic = isArabic, enabled = !hasLeft) { prefix ->
                if (!hasLeft) {
                    body = insertEditorAction(body, prefix)
                    saveDraft()
                }
            }
            BasicTextField(
                value = body,
                onValueChange = { value ->
                    if (!hasLeft) {
                        val changed = body.text != value.text
                        body = value
                        if (changed) saveDraft()
                    }
                },
                readOnly = hasLeft,
                modifier = Modifier.fillMaxWidth().heightIn(min = 420.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (body.text.isBlank()) Text(labels.writeHere, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
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
