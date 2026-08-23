package com.yonte.feature.notes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class EditorAction(val arabic: String, val english: String) {
    HEADING("عنوان", "Heading"),
    BULLET("نقطة", "Bullet"),
    CHECKBOX("مهمة", "Task"),
    DIVIDER("فاصل", "Divider"),
}

@Composable
internal fun EditorToolbar(isArabic: Boolean, onAction: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EditorAction.entries.forEach { action ->
            FilterChip(
                selected = false,
                onClick = {
                    val prefix = when (action) {
                        EditorAction.HEADING -> "# "
                        EditorAction.BULLET -> "- "
                        EditorAction.CHECKBOX -> "- [ ] "
                        EditorAction.DIVIDER -> "---\n"
                    }
                    onAction(prefix)
                },
                label = { Text(if (isArabic) action.arabic else action.english) },
            )
        }
    }
}

internal fun appendEditorAction(body: String, prefix: String): String {
    val separator = if (body.isBlank() || body.endsWith("\n")) "" else "\n"
    return body + separator + prefix
}
