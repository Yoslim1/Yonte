package com.yonte.feature.notes

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Offsets always refer to the stored Markdown, including its original line endings. */
internal data class EditorBlock(
    val start: Int,
    val end: Int,
    val contentStart: Int = start,
    val checkOffset: Int? = null,
    val checked: Boolean = false,
)

private val taskLine = Regex("^[ \\t]*[-*+] \\[([ xX])\\](?:[ \\t]|$)")

internal fun editorBlocks(text: String): List<EditorBlock> {
    val blocks = mutableListOf<EditorBlock>()
    var lineStart = 0
    var plainStart = 0
    var fence: Char? = null
    var fenceLength = 0
    while (lineStart <= text.length) {
        val newline = text.indexOf('\n', lineStart).let { if (it < 0) text.length else it }
        val lineEnd = if (newline > lineStart && text[newline - 1] == '\r') newline - 1 else newline
        val line = text.substring(lineStart, lineEnd)
        val trimmed = line.trimStart()
        val marker = trimmed.firstOrNull()
        val markerLength = trimmed.takeWhile { it == marker }.length
        val isFence = marker in listOf('`', '~') && markerLength >= 3
        val match = if (fence == null && !isFence) taskLine.find(line) else null
        if (isFence) {
            if (fence == null) { fence = marker; fenceLength = markerLength }
            else if (marker == fence && markerLength >= fenceLength && trimmed.drop(markerLength).isBlank()) fence = null
        }
        if (match != null) {
            if (plainStart < lineStart) {
                // The separator belongs to the document, not either editable field.
                val end = (lineStart - 1).let { if (it > plainStart && text[it - 1] == '\r') it - 1 else it }
                blocks += EditorBlock(plainStart, end)
            }
            val checkOffset = lineStart + match.groups[1]!!.range.first
            blocks += EditorBlock(lineStart, lineEnd, lineStart + match.value.length, checkOffset, text[checkOffset] != ' ')
            plainStart = (newline + 1).coerceAtMost(text.length)
        }
        if (newline == text.length) break
        lineStart = newline + 1
    }
    if (plainStart < text.length || blocks.isEmpty() || text.endsWith('\n')) {
        blocks += EditorBlock(plainStart, text.length)
    }
    return blocks
}

internal fun replaceEditorBlock(value: TextFieldValue, block: EditorBlock, replacement: TextFieldValue): TextFieldValue {
    val previous = value.text.substring(block.contentStart, block.end)
    val cursor = replacement.selection.start
    val enter = block.checkOffset != null && replacement.selection.collapsed && replacement.composition == null &&
        replacement.text.length == previous.length + 1 && cursor > 0 && replacement.text[cursor - 1] == '\n' &&
        replacement.text.removeRange(cursor - 1, cursor) == previous
    // Enter on an empty task exits the list without leaving an invisible empty task.
    if (enter && previous.isEmpty()) {
        return TextFieldValue(
            text = value.text.removeRange(block.start, block.end),
            selection = TextRange(block.start),
        )
    }
    val separator = if (value.text.contains("\r\n")) "\r\n" else "\n"
    val indentation = value.text.substring(block.start, block.contentStart).takeWhile { it == ' ' || it == '\t' }
    val continuation = separator + indentation + "- [ ] "
    val edited = if (enter) replacement.copy(
        text = replacement.text.substring(0, cursor - 1) + continuation + replacement.text.substring(cursor),
        selection = TextRange(cursor - 1 + continuation.length),
    ) else replacement
    return TextFieldValue(
        text = value.text.replaceRange(block.contentStart, block.end, edited.text),
        selection = TextRange(block.contentStart + edited.selection.start, block.contentStart + edited.selection.end),
        composition = edited.composition?.let { TextRange(block.contentStart + it.start, block.contentStart + it.end) },
    )
}

internal fun toggleEditorTask(value: TextFieldValue, block: EditorBlock): TextFieldValue {
    val offset = block.checkOffset ?: return value
    return value.copy(text = value.text.replaceRange(offset, offset + 1, if (block.checked) " " else "x"), composition = null)
}

internal fun editorBlockValue(value: TextFieldValue, block: EditorBlock): TextFieldValue {
    fun local(offset: Int) = (offset - block.contentStart).coerceIn(0, block.end - block.contentStart)
    return TextFieldValue(
        text = value.text.substring(block.contentStart, block.end),
        selection = TextRange(local(value.selection.start), local(value.selection.end)),
        composition = value.composition?.takeIf { it.min >= block.contentStart && it.max <= block.end }
            ?.let { TextRange(local(it.start), local(it.end)) },
    )
}
