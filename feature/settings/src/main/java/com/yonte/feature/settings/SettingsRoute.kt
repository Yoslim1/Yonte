package com.yonte.feature.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yonte.core.backup.BackupGateway
import com.yonte.core.backup.BackupNote
import com.yonte.core.database.ArabicNormalizer
import com.yonte.core.database.NoteEntity
import com.yonte.core.database.NoteRepository
import com.yonte.core.update.UpdateGateway
import com.yonte.core.update.UpdateInfo
import kotlinx.coroutines.launch

private enum class SettingsSection { APPEARANCE, DATA, UPDATES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    repository: NoteRepository,
    backupGateway: BackupGateway,
    updateGateway: UpdateGateway,
    darkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    currentVersionCode: Int,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isArabic = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    var section by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateStatus by remember { mutableStateOf("") }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                backupGateway.exportNotes(context.contentResolver, uri, repository.getAll().map { note ->
                    BackupNote(note.id, note.title, note.body, note.isPinned, note.createdAt, note.updatedAt)
                })
            }.onSuccess { Toast.makeText(context, if (isArabic) "تم تصدير النسخة" else "Backup exported", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, if (isArabic) "فشل تصدير النسخة" else "Backup export failed", Toast.LENGTH_SHORT).show() }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                backupGateway.importNotes(context.contentResolver, uri).map { item ->
                    NoteEntity(item.id, item.title, item.body, ArabicNormalizer.normalize("${item.title} ${item.body}"), item.isPinned, false, false, item.createdAt, item.updatedAt)
                }
            }.onSuccess { restored ->
                repository.restore(restored)
                Toast.makeText(context, if (isArabic) "تم استيراد النسخة" else "Backup imported", Toast.LENGTH_SHORT).show()
            }.onFailure { Toast.makeText(context, if (isArabic) "فشل استيراد النسخة" else "Backup import failed", Toast.LENGTH_SHORT).show() }
        }
    }

    val title = when (section) {
        null -> if (isArabic) "الإعدادات" else "Settings"
        SettingsSection.APPEARANCE -> if (isArabic) "المظهر" else "Appearance"
        SettingsSection.DATA -> if (isArabic) "البيانات والنسخ الاحتياطي" else "Data & backup"
        SettingsSection.UPDATES -> if (isArabic) "التحديثات" else "Updates"
    }
    BackHandler(enabled = section != null) { section = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    TextButton(onClick = { if (section == null) onClose() else section = null }) {
                        Text(if (section == null) (if (isArabic) "إغلاق" else "Close") else (if (isArabic) "رجوع" else "Back"))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (section) {
                null -> SettingsMenu(isArabic, { section = SettingsSection.APPEARANCE }, { section = SettingsSection.DATA }, { section = SettingsSection.UPDATES })
                SettingsSection.APPEARANCE -> SettingsAppearance(darkTheme, onThemeChanged, isArabic)
                SettingsSection.DATA -> SettingsData({ exportLauncher.launch("yonte-backup.ynt") }, { importLauncher.launch(arrayOf("application/octet-stream", "application/json", "*/*")) }, isArabic)
                SettingsSection.UPDATES -> SettingsUpdates(
                    status = updateStatus,
                    info = updateInfo,
                    onCheck = {
                        updateStatus = if (isArabic) "جارٍ التحقق…" else "Checking…"
                        scope.launch {
                            updateGateway.checkForUpdate(currentVersionCode)
                                .onSuccess { found -> updateInfo = found; updateStatus = if (found == null) (if (isArabic) "أنت على أحدث إصدار" else "You are up to date") else (if (isArabic) "يتوفر تحديث" else "Update available") }
                                .onFailure { updateStatus = if (isArabic) "فشل التحقق من التحديث" else "Update check failed" }
                        }
                    },
                    onDownload = { info ->
                        updateStatus = if (isArabic) "جارٍ التنزيل…" else "Downloading…"
                        scope.launch {
                            updateGateway.downloadAndVerify(info)
                                .onSuccess { uri -> updateStatus = if (isArabic) "تم التحقق" else "Verified"; updateGateway.install(uri) }
                                .onFailure { updateStatus = if (isArabic) "فشل التحقق من الملف" else "Download verification failed" }
                        }
                    },
                    isArabic = isArabic,
                )
            }
        }
    }
}

@Composable
private fun SettingsMenu(isArabic: Boolean, onAppearance: () -> Unit, onData: () -> Unit, onUpdates: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (isArabic) "خصّص تجربتك" else "Customize your experience", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SettingsMenuRow(if (isArabic) "المظهر" else "Appearance", if (isArabic) "الوضع الداكن والألوان" else "Theme and colors", onAppearance)
        SettingsMenuRow(if (isArabic) "البيانات" else "Data", if (isArabic) "تصدير واستيراد نسخة محلية" else "Export and import a local backup", onData)
        SettingsMenuRow(if (isArabic) "التحديثات" else "Updates", if (isArabic) "البحث عن إصدار أحدث" else "Check for a newer version", onUpdates)
    }
}

@Composable
private fun SettingsMenuRow(title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsAppearance(darkTheme: Boolean, onThemeChanged: (Boolean) -> Unit, isArabic: Boolean) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(if (isArabic) "الوضع الداكن" else "Dark mode")
                    Text(if (isArabic) "مظهر مريح للعين" else "A softer theme for your eyes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = darkTheme, onCheckedChange = onThemeChanged)
            }
        }
    }
}

@Composable
private fun SettingsData(onExport: () -> Unit, onImport: () -> Unit, isArabic: Boolean) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(if (isArabic) "بياناتك محلية على جهازك" else "Your data stays on this device", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onExport, modifier = Modifier.weight(1f)) { Text(if (isArabic) "تصدير" else "Export") }
            OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text(if (isArabic) "استيراد" else "Import") }
        }
    }
}

@Composable
private fun SettingsUpdates(status: String, info: UpdateInfo?, onCheck: () -> Unit, onDownload: (UpdateInfo) -> Unit, isArabic: Boolean) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onCheck, modifier = Modifier.fillMaxWidth()) { Text(if (isArabic) "التحقق من وجود تحديث" else "Check for updates") }
        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        info?.let { update ->
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isArabic) "إصدار جديد: ${update.versionName}" else "New version: ${update.versionName}", style = MaterialTheme.typography.titleMedium)
                    Text(update.releaseNotes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { onDownload(update) }) { Text(if (isArabic) "تنزيل وتثبيت" else "Download and install") }
                }
            }
        }
    }
}
