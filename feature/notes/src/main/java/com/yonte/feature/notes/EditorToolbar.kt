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
internal fun EditorToolbar(isArabic: Boolean, enabled: Boolean = true, onAction: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EditorAction.entries.forEach { action ->
            FilterChip(
                selected = false,
                enabled = enabled,
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

/** Apply a block action at the current line, preserving the selected text. */
internal fun insertEditorAction(value: androidx.compose.ui.text.input.TextFieldValue, prefix: String): androidx.compose.ui.text.input.TextFieldValue {
    val start = value.selection.min
    val lineStart = value.text.lastIndexOf('\n', (start - 1).coerceAtLeast(0))
        .let { if (start == 0) 0 else it + 1 }
    if (prefix == "- [ ] " && editorBlocks(value.text).any { it.start == lineStart && it.checkOffset != null }) {
        val lineEnd = value.text.indexOf('\n', lineStart).let { if (it < 0) value.text.length else it }
        val contentEnd = if (lineEnd > lineStart && value.text[lineEnd - 1] == '\r') lineEnd - 1 else lineEnd
        val separator = if (contentEnd < lineEnd ||
            (lineEnd == value.text.length && value.text.contains("\r\n"))
        ) "\r\n" else "\n"
        val inserted = separator + prefix
        return value.copy(
            text = value.text.substring(0, contentEnd) + inserted + value.text.substring(contentEnd),
            selection = androidx.compose.ui.text.TextRange(contentEnd + inserted.length),
            composition = null,
        )
    }
    val text = value.text.substring(0, lineStart) + prefix + value.text.substring(lineStart)
    return value.copy(text = text, selection = androidx.compose.ui.text.TextRange(
        value.selection.start + prefix.length, value.selection.end + prefix.length,
    ), composition = null)
}
