package com.yonte.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsSecurity(
    currentMethod: String,
    isArabic: Boolean,
    onChangeMethod: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = if (isArabic) "رجوع" else "Back")
            }
            Text(
                if (isArabic) "طريقة فتح التطبيق" else "Unlock method",
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        Text(
            if (isArabic) "اختر كيف تريد فتح التطبيق" else "Choose how you want to unlock the app",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        UnlockMethodOption(
            title = if (isArabic) "كلمة السر" else "Passphrase",
            description = if (isArabic) "أقوى حماية، تحتاج تذكرها" else "Strongest protection, you need to remember it",
            selected = currentMethod == "PASSPHRASE",
            onClick = { onChangeMethod("PASSPHRASE") },
        )

        UnlockMethodOption(
            title = if (isArabic) "الرمز" else "PIN",
            description = if (isArabic) "سريع وسهل، 4-6 أرقام" else "Fast and easy, 4-6 digits",
            selected = currentMethod == "PIN",
            onClick = { onChangeMethod("PIN") },
        )

        UnlockMethodOption(
            title = if (isArabic) "البصمة" else "Biometric",
            description = if (isArabic) "الأسرع، يحتاج بصمة مسجلة" else "Fastest, requires enrolled fingerprint",
            selected = currentMethod == "BIOMETRIC",
            onClick = { onChangeMethod("BIOMETRIC") },
        )
    }
}

@Composable
private fun UnlockMethodOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}
