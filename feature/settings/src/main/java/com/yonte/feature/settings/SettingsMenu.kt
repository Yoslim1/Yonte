package com.yonte.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsMenu(isArabic: Boolean, onAppearance: () -> Unit, onData: () -> Unit, onUpdates: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Yonte", style = MaterialTheme.typography.headlineSmall)
                Text(if (isArabic) "إعدادات بسيطة، تحكم واضح" else "Simple settings, clear control", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(if (isArabic) "خصّص تجربتك" else "Customize your experience", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SettingsMenuRow(if (isArabic) "المظهر" else "Appearance", if (isArabic) "الوضع الداكن والألوان" else "Theme and colors", Icons.Outlined.Palette, onAppearance)
        SettingsMenuRow(if (isArabic) "البيانات" else "Data", if (isArabic) "تصدير واستيراد نسخة محلية" else "Export and import a local backup", Icons.Outlined.FolderZip, onData)
        SettingsMenuRow(if (isArabic) "التحديثات" else "Updates", if (isArabic) "البحث عن إصدار أحدث" else "Check for a newer version", Icons.Outlined.SystemUpdateAlt, onUpdates)
    }
}

@Composable
internal fun SettingsMenuRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
