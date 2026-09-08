package com.yonte.feature.notes

import java.text.DateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yonte.core.database.NoteEntity

@Composable
internal fun NoteCard(
    note: NoteEntity,
    isArabic: Boolean,
    onEdit: (NoteEntity) -> Unit,
    onPin: (NoteEntity) -> Unit,
    onArchive: (NoteEntity) -> Unit,
    onTrash: (NoteEntity) -> Unit,
    onRestore: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val preview = notePreview(note.title, note.body)
    val updatedLabel = remember(note.updatedAt, isArabic) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale(if (isArabic) "ar" else "en"))
            .format(Date(note.updatedAt))
    }
    var menuExpanded by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, label = "Note press")
    Card(
        onClick = { if (!note.isTrashed) onEdit(note) },
        modifier = modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale },
        interactionSource = interaction,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (note.isPinned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f) else MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 0.dp, focusedElevation = 5.dp),
    ) {
        Column(Modifier.background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f), Color.Transparent))).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(preview.title.ifBlank { if (isArabic) "بدون عنوان" else "Untitled" }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (note.isPinned) Icon(Icons.Outlined.PushPin, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = if (isArabic) "المزيد" else "More", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        if (note.isTrashed) {
                            DropdownMenuItem(
                                text = { Text(if (isArabic) (if (note.isArchived) "استعادة إلى الأرشيف" else "استعادة الملاحظة") else (if (note.isArchived) "Restore to archive" else "Restore note")) },
                                onClick = { menuExpanded = false; onRestore(note) },
                            )
                        } else {
                            DropdownMenuItem(text = { Text(if (note.isPinned) (if (isArabic) "إلغاء التثبيت" else "Unpin") else (if (isArabic) "تثبيت" else "Pin")) }, onClick = { menuExpanded = false; onPin(note) })
                            DropdownMenuItem(
                                text = { Text(if (note.isArchived) (if (isArabic) "إلغاء الأرشفة" else "Unarchive") else (if (isArabic) "أرشفة" else "Archive")) },
                                onClick = { menuExpanded = false; if (note.isArchived) onRestore(note) else onArchive(note) },
                            )
                            DropdownMenuItem(text = { Text(if (isArabic) "نقل إلى المحذوفات" else "Move to trash") }, onClick = { menuExpanded = false; onTrash(note) })
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(preview.body.ifBlank { if (isArabic) "ملاحظة فارغة" else "Empty note" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (preview.tags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(preview.tags.joinToString("  ") { "#$it" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(updatedLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            }
        }
    }
}
