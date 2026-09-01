package com.yonte.feature.settings

import android.net.Uri
import com.yonte.core.backup.BackupFrequency
import com.yonte.core.update.UpdateInfo

internal enum class SettingsSection { APPEARANCE, DATA, UPDATES }

internal data class SettingsUiState(
    val section: SettingsSection? = null,
    val updateStatus: String = "",
    val updateInfo: UpdateInfo? = null,
    val destinationUri: String? = null,
    val frequency: BackupFrequency = BackupFrequency.OFF,
    val backupSizeBytes: Long = 0L,
    val showImportPassphraseDialog: Boolean = false,
    val pendingImportUri: Uri? = null,
)
