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
internal fun NotesEmptyState(
    onNew: (String) -> Unit,
    isArabic: Boolean,
    collection: NotesCollection,
    hasFilters: Boolean,
    onClearFilters: () -> Unit,
) {
    val title = when {
        hasFilters -> if (isArabic) "لا توجد نتائج" else "No results"
        collection == NotesCollection.ARCHIVED -> if (isArabic) "الأرشيف فارغ" else "Archive is empty"
        collection == NotesCollection.TRASHED -> if (isArabic) "المحذوفات فارغة" else "Trash is empty"
        else -> if (isArabic) "مساحة لفكرتك الأولى" else "Room for your first idea"
    }
    val message = when {
        hasFilters -> if (isArabic) "جرّب كلمات أخرى أو امسح عوامل التصفية" else "Try different words or clear your filters"
        collection == NotesCollection.ARCHIVED -> if (isArabic) "ستجد ملاحظاتك المؤرشفة هنا ويمكنك استعادتها" else "Archived notes appear here and can be restored"
        collection == NotesCollection.TRASHED -> if (isArabic) "ستجد الملاحظات المحذوفة هنا ويمكنك استعادتها" else "Deleted notes appear here and can be restored"
        else -> if (isArabic) "اكتب ما يلهمك، وسيحفظه Yonte تلقائياً" else "Capture what inspires you; Yonte saves as you go"
    }
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            if (hasFilters) {
                TextButton(onClick = onClearFilters) { Text(if (isArabic) "مسح التصفية" else "Clear filters") }
            } else if (collection == NotesCollection.ACTIVE) {
                TextButton(onClick = { onNew("") }) { Text(if (isArabic) "أنشئ أول ملاحظة" else "Create your first note") }
            }
        }
    }
}
