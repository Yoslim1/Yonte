package com.yonte.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsAppearance(
    darkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    isArabic: Boolean,
    languageCode: String? = null,
    onLanguageChanged: (String) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (isArabic) "الوضع الداكن" else "Dark mode", style = MaterialTheme.typography.titleMedium)
                    Text(if (isArabic) "اختر المظهر المناسب لك" else "Choose your preferred appearance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = darkTheme, onCheckedChange = onThemeChanged)
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(if (isArabic) "لغة التطبيق" else "App language", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = languageCode == "ar" || (languageCode == null && isArabic),
                        onClick = { onLanguageChanged("ar") },
                        label = { Text("العربية") },
                    )
                    FilterChip(
                        selected = languageCode == "en" || (languageCode == null && !isArabic),
                        onClick = { onLanguageChanged("en") },
                        label = { Text("English") },
                    )
                }
            }
        }
    }
}
