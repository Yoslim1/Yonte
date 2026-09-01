package com.yonte.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yonte.core.update.UpdateInfo

@Composable
internal fun SettingsUpdates(status: String, info: UpdateInfo?, onCheck: () -> Unit, onDownload: (UpdateInfo) -> Unit, isArabic: Boolean) {
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
