package com.yonte.feature.notes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/** A presentation of the same draft, not a second task store. */
@Composable
internal fun TaskBodyEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    readOnly: Boolean,
    isArabic: Boolean,
    placeholder: String,
    focusRequest: Int,
) {
    val blocks = remember(value.text) { editorBlocks(value.text) }
    val tasks = blocks.filter { it.checkOffset != null }
    var sourceMode by remember { mutableStateOf(false) }
    var editFocusRequest by remember { mutableIntStateOf(0) }
    LaunchedEffect(focusRequest) { if (focusRequest > 0) sourceMode = false }
    val displayedBlocks = if (sourceMode) listOf(EditorBlock(0, value.text.length)) else blocks
    val selectedIndex = displayedBlocks.indexOfLast { value.selection.start >= it.start }.coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().heightIn(min = 420.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (tasks.isNotEmpty()) {
            val completed = tasks.count { it.checked }
            val progress by animateFloatAsState(completed.toFloat() / tasks.size, label = "Task progress")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isArabic) "المهام المكتملة: $completed من ${tasks.size}" else "$completed of ${tasks.size} tasks completed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    sourceMode = !sourceMode
                    editFocusRequest += 1
                }, enabled = !readOnly) {
                    Text(if (sourceMode) (if (isArabic) "عرض المهام" else "Show tasks") else (if (isArabic) "تحرير النص الكامل" else "Edit full text"))
                }
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
        displayedBlocks.forEachIndexed { index, block ->
            val requester = remember { FocusRequester() }
            LaunchedEffect(focusRequest, editFocusRequest, sourceMode) {
                if ((focusRequest > 0 || editFocusRequest > 0) && index == selectedIndex && !readOnly) requester.requestFocus()
            }
            val task = block.checkOffset != null
            val field: @Composable (Modifier) -> Unit = { modifier ->
                BasicTextField(
                    value = editorBlockValue(value, block),
                    onValueChange = { replacement ->
                        if (!readOnly) {
                            val updated = replaceEditorBlock(value, block, replacement)
                            if (!sourceMode && updated.text != value.text &&
                                editorBlocks(updated.text).map { it.checkOffset != null } != blocks.map { it.checkOffset != null }
                            ) {
                                editFocusRequest += 1
                            }
                            onValueChange(updated)
                        }
                    },
                    readOnly = readOnly,
                    modifier = modifier.heightIn(min = 48.dp).focusRequester(requester).onFocusChanged { state ->
                        if (state.isFocused && index != selectedIndex) {
                            onValueChange(value.copy(selection = TextRange(block.contentStart), composition = null))
                        }
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = if (block.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (block.checked) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        Box(Modifier.padding(vertical = 10.dp)) {
                            if (block.contentStart == block.end) {
                                Text(if (task) (if (isArabic) "اكتب المهمة…" else "Write a task…") else placeholder,
                                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            inner()
                        }
                    },
                )
            }
            if (task) {
                val surface by animateColorAsState(
                    if (block.checked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                    label = "Task completion surface",
                )
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = surface,
                    tonalElevation = 1.dp,
                    shadowElevation = if (block.checked) 0.dp else 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(Modifier.fillMaxWidth().padding(end = 14.dp), verticalAlignment = Alignment.Top) {
                        Checkbox(
                            checked = block.checked,
                            enabled = !readOnly,
                            onCheckedChange = { onValueChange(toggleEditorTask(value, block)) },
                            modifier = Modifier.semantics {
                                contentDescription = value.text.substring(block.contentStart, block.end).ifBlank { if (isArabic) "مهمة" else "Task" }
                            },
                        )
                        field(Modifier.weight(1f))
                    }
                }
            } else {
                field(Modifier.fillMaxWidth())
            }
        }
    }
}
