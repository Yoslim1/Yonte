package com.yonte.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun NotesViewModeToggle(mode: NotesViewMode, isArabic: Boolean, onModeChange: (NotesViewMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(
            selected = mode == NotesViewMode.LIST,
            onClick = { onModeChange(NotesViewMode.LIST) },
            label = { Text(if (isArabic) "قائمة" else "List") },
            leadingIcon = { Icon(Icons.Outlined.ViewList, contentDescription = null, modifier = Modifier.size(16.dp)) },
        )
        FilterChip(
            selected = mode == NotesViewMode.GRID,
            onClick = { onModeChange(NotesViewMode.GRID) },
            label = { Text(if (isArabic) "شبكة" else "Grid") },
            leadingIcon = { Icon(Icons.Outlined.GridView, contentDescription = null, modifier = Modifier.size(16.dp)) },
        )
    }
}
