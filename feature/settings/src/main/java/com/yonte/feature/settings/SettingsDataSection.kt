package com.yonte.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yonte.core.backup.BackupFrequency
import com.yonte.core.security.LocalKeyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsData(
    uiState: SettingsUiState,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onFolderPickerResult: (Uri) -> Unit,
    onFrequencyChanged: (BackupFrequency) -> Unit,
    localKeyManager: LocalKeyManager,
    isArabic: Boolean,
) {
    var frequencyExpanded by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            onFolderPickerResult(uri)
        }
    }

    val frequencyLabels = mapOf(
        BackupFrequency.WEEKLY to (if (isArabic) "أسبوعيًا" else "Weekly"),
        BackupFrequency.BIWEEKLY to (if (isArabic) "كل أسبوعين" else "Every two weeks"),
        BackupFrequency.MONTHLY to (if (isArabic) "شهريًا" else "Monthly"),
        BackupFrequency.OFF to (if (isArabic) "متوقف" else "Off"),
    )

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(if (isArabic) "بياناتك محلية على جهازك" else "Your data stays on this device", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onExport, modifier = Modifier.weight(1f)) { Text(if (isArabic) "تصدير" else "Export") }
            OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text(if (isArabic) "استيراد" else "Import") }
        }

        Spacer(Modifier.height(8.dp))
        Text(if (isArabic) "النسخ الاحتياطي التلقائي" else "Automatic backup", style = MaterialTheme.typography.titleMedium)
        Text(
            if (isArabic) "نسخ احتياطي دوري للStored notes في مجلد تختاره" else "Periodically back up your notes to a folder you choose",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedButton(onClick = { folderPickerLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (uiState.destinationUri != null) (if (isArabic) "تغيير المجلد" else "Change folder") else (if (isArabic) "اختيار مجلد النسخ الاحتياطي" else "Choose backup folder"))
        }

        ExposedDropdownMenuBox(expanded = frequencyExpanded, onExpandedChange = { frequencyExpanded = it }) {
            OutlinedTextField(
                value = frequencyLabels[uiState.frequency] ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(if (isArabic) "التكرار" else "Frequency") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = frequencyExpanded, onDismissRequest = { frequencyExpanded = false }) {
                BackupFrequency.entries.forEach { entry ->
                    DropdownMenuItem(
                        text = { Text(frequencyLabels[entry] ?: entry.name) },
                        onClick = {
                            frequencyExpanded = false
                            onFrequencyChanged(entry)
                        },
                    )
                }
            }
        }

        if (uiState.backupSizeBytes > 0) {
            Text(
                if (isArabic) "حجم النسخ المحفوظة: ${formatSize(uiState.backupSizeBytes)}" else "Saved backups size: ${formatSize(uiState.backupSizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}
