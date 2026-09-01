package com.yonte.feature.settings

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yonte.core.backup.AutoBackupScheduler
import com.yonte.core.backup.BackupFrequency
import com.yonte.core.backup.BackupGateway
import com.yonte.core.backup.BackupNote
import com.yonte.core.backup.ScheduledBackupWorker
import com.yonte.core.database.ArabicNormalizer
import com.yonte.core.database.NoteEntity
import com.yonte.core.database.NoteRepository
import com.yonte.core.security.LocalKeyManager
import com.yonte.core.update.UpdateGateway
import com.yonte.core.update.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SettingsViewModel(
    private val repository: NoteRepository,
    private val backupGateway: BackupGateway,
    private val updateGateway: UpdateGateway,
    private val localKeyManager: LocalKeyManager,
    private val currentVersionCode: Int,
    private val appContext: Context,
) : ViewModel() {
    private val prefs = appContext.getSharedPreferences(ScheduledBackupWorker.PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            destinationUri = prefs.getString(ScheduledBackupWorker.KEY_DESTINATION_URI, null),
            frequency = runCatching {
                BackupFrequency.valueOf(prefs.getString(KEY_FREQUENCY, BackupFrequency.OFF.name)!!)
            }.getOrDefault(BackupFrequency.OFF),
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun openSection(section: SettingsSection?) {
        _uiState.value = _uiState.value.copy(section = section)
    }

    fun export(contentResolver: ContentResolver, uri: Uri, isArabic: Boolean, onResult: (Boolean) -> Unit) {
        val sessionKey = localKeyManager.cachedSessionKey()
        val localSalt = localKeyManager.currentSalt()
        if (sessionKey == null || localSalt == null) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            try {
                runCatching {
                    PassphraseBackupFlow.exportWithKey(
                        backupGateway,
                        contentResolver,
                        uri,
                        repository.getAll().map { note ->
                            BackupNote(note.id, note.title, note.body, note.isPinned, note.createdAt, note.updatedAt)
                        },
                        sessionKey,
                        localSalt,
                    )
                }.onSuccess { onResult(true) }
                    .onFailure { onResult(false) }
            } finally {
                // sessionKey is not our copy to clear; it lives in LocalKeyManager
            }
        }
    }

    fun import(contentResolver: ContentResolver, uri: Uri, passphrase: CharArray, isArabic: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                runCatching {
                    PassphraseBackupFlow.import(backupGateway, contentResolver, uri, passphrase).map { item ->
                        NoteEntity(item.id, item.title, item.body, ArabicNormalizer.normalize("${item.title} ${item.body}"), item.isPinned, false, false, item.createdAt, item.updatedAt)
                    }
                }.onSuccess { restored ->
                    repository.restore(restored)
                    onResult(true)
                }.onFailure { e ->
                    android.util.Log.e("YonteBackupImport", "Import failed", e)
                    onResult(false)
                }
            } finally {
                passphrase.fill('\u0000')
            }
        }
    }

    fun showImportDialog(uri: Uri) {
        _uiState.value = _uiState.value.copy(showImportPassphraseDialog = true, pendingImportUri = uri)
    }

    fun dismissImportDialog() {
        _uiState.value = _uiState.value.copy(showImportPassphraseDialog = false, pendingImportUri = null)
    }

    fun checkForUpdate(isArabic: Boolean) {
        _uiState.value = _uiState.value.copy(updateStatus = if (isArabic) "جارٍ التحقق…" else "Checking…")
        viewModelScope.launch {
            updateGateway.checkForUpdate(currentVersionCode)
                .onSuccess { found ->
                    _uiState.value = _uiState.value.copy(
                        updateInfo = found,
                        updateStatus = if (found == null) (if (isArabic) "أنت على أحدث إصدار" else "You are up to date") else (if (isArabic) "يتوفر تحديث" else "Update available"),
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(updateStatus = if (isArabic) "فشل التحقق من التحديث" else "Update check failed")
                }
        }
    }

    fun downloadUpdate(info: UpdateInfo, isArabic: Boolean) {
        _uiState.value = _uiState.value.copy(updateStatus = if (isArabic) "جارٍ التنزيل…" else "Downloading…")
        viewModelScope.launch {
            updateGateway.downloadAndVerify(info)
                .onSuccess { uri ->
                    _uiState.value = _uiState.value.copy(updateStatus = if (isArabic) "تم التحقق" else "Verified")
                    updateGateway.install(uri)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(updateStatus = if (isArabic) "فشل التحقق من الملف" else "Download verification failed")
                }
        }
    }

    fun setAutoBackupDestination(uri: Uri, contentResolver: ContentResolver) {
        contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        prefs.edit().putString(ScheduledBackupWorker.KEY_DESTINATION_URI, uri.toString()).apply()
        _uiState.value = _uiState.value.copy(destinationUri = uri.toString())
        val freq = _uiState.value.frequency
        if (freq != BackupFrequency.OFF) {
            AutoBackupScheduler.schedule(appContext, freq)
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(backupSizeBytes = calculateBackupSize(appContext, uri))
        }
    }

    fun setAutoBackupFrequency(frequency: BackupFrequency, contentResolver: ContentResolver) {
        prefs.edit().putString(KEY_FREQUENCY, frequency.name).apply()
        AutoBackupScheduler.schedule(appContext, frequency)
        _uiState.value = _uiState.value.copy(frequency = frequency)
        if (frequency == BackupFrequency.OFF) {
            localKeyManager.clearAutoBackupKey()
        } else {
            val destUri = _uiState.value.destinationUri
            if (destUri != null) {
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        backupSizeBytes = calculateBackupSize(appContext, Uri.parse(destUri)),
                    )
                }
            }
        }
    }

    private suspend fun calculateBackupSize(context: Context, treeUri: Uri): Long = withContext(Dispatchers.IO) {
        val tree = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext 0L
        tree.listFiles().sumOf { it.length() }
    }

    private companion object {
        const val KEY_FREQUENCY = "auto_backup_frequency"
    }
}
