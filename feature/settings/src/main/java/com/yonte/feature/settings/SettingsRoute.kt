package com.yonte.feature.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.yonte.core.backup.BackupGateway
import com.yonte.core.database.NoteRepository
import com.yonte.core.security.LocalKeyManager
import com.yonte.core.update.UpdateGateway

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
    localKeyManager: LocalKeyManager,
    isVisible: Boolean = true,
    languageCode: String? = null,
    onLanguageChanged: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val store = remember(repository, backupGateway, updateGateway, localKeyManager, currentVersionCode) { ViewModelStore() }
    val viewModel = remember(store) {
        SettingsViewModel(repository, backupGateway, updateGateway, localKeyManager, currentVersionCode, context.applicationContext)
            .also { store.put("settings", it) }
    }
    DisposableEffect(store) { onDispose { store.clear() } }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isArabic = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            viewModel.export(context.contentResolver, uri, isArabic) { success ->
                Toast.makeText(context, if (success) (if (isArabic) "تم تصدير النسخة" else "Backup exported") else (if (isArabic) "فشل تصدير النسخة" else "Backup export failed"), Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.showImportDialog(uri)
        }
    }

    val title = when (uiState.section) {
        null -> if (isArabic) "الإعدادات" else "Settings"
        SettingsSection.DATA -> if (isArabic) "البيانات والنسخ الاحتياطي" else "Data & backup"
        SettingsSection.UPDATES -> if (isArabic) "التحديثات" else "Updates"
    }
    BackHandler(enabled = isVisible && uiState.section != null) { viewModel.openSection(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    TextButton(onClick = { if (uiState.section == null) onClose() else viewModel.openSection(null) }) {
                        Text(if (uiState.section == null) (if (isArabic) "إغلاق" else "Close") else (if (isArabic) "رجوع" else "Back"))
                    }
                },
                actions = {
                    if (uiState.section != null) {
                        TextButton(onClick = onClose) { Text(if (isArabic) "إغلاق" else "Close") }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (uiState.section) {
                null -> SettingsMenu(
                    isArabic = isArabic,
                    darkTheme = darkTheme,
                    onThemeChanged = onThemeChanged,
                    languageCode = languageCode,
                    onLanguageChanged = onLanguageChanged,
                    onData = { viewModel.openSection(SettingsSection.DATA) },
                    onUpdates = { viewModel.openSection(SettingsSection.UPDATES) },
                )
                SettingsSection.DATA -> SettingsData(
                    uiState = uiState,
                    onExport = { exportLauncher.launch("yonte-backup.ynt") },
                    onImport = { importLauncher.launch(arrayOf("application/octet-stream", "application/json", "*/*")) },
                    onFolderPickerResult = { uri -> viewModel.setAutoBackupDestination(uri, context.contentResolver) },
                    onFrequencyChanged = { freq -> viewModel.setAutoBackupFrequency(freq, context.contentResolver) },
                    localKeyManager = localKeyManager,
                    isArabic = isArabic,
                )
                SettingsSection.UPDATES -> SettingsUpdates(
                    status = uiState.updateStatus,
                    info = uiState.updateInfo,
                    onCheck = { viewModel.checkForUpdate(isArabic) },
                    onDownload = { info -> viewModel.downloadUpdate(info, isArabic) },
                    isArabic = isArabic,
                )
            }
        }
    }

    if (uiState.showImportPassphraseDialog) {
        BackupPassphraseDialog(
            mode = BackupPassphraseMode.IMPORT,
            isArabic = isArabic,
            onConfirm = { passphrase ->
                val importUri = uiState.pendingImportUri
                viewModel.dismissImportDialog()
                if (importUri != null) {
                    viewModel.import(context.contentResolver, importUri, passphrase, isArabic) { success ->
                        Toast.makeText(context, if (success) (if (isArabic) "تم استيراد النسخة" else "Backup imported") else (if (isArabic) "فشل استيراد النسخة" else "Backup import failed"), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    passphrase.fill('\u0000')
                }
            },
            onDismiss = { viewModel.dismissImportDialog() },
        )
    }
}
