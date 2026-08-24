package com.yonte.feature.notes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun NotesEmptyState(onNew: (String) -> Unit, isArabic: Boolean) {
    Box(Modifier.fillMaxWidth().padding(vertical = 72.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (isArabic) "مساحة جديدة" else "A fresh space", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(if (isArabic) "ابدأ بفكرة صغيرة، وسيحفظها Yonte تلقائياً" else "Start small; Yonte saves as you go", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { onNew("") }) { Text(if (isArabic) "أنشئ أول ملاحظة" else "Create your first note") }
        }
    }
}
