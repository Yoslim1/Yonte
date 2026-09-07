package com.yonte.feature.notes

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorToolbarTest {
    @Test
    fun appendsActionToEmptyBodyWithoutLeadingBlankLine() {
        assertEquals("# ", appendEditorAction("", "# "))
    }

    @Test
    fun appendsActionOnNewLineToExistingBody() {
        assertEquals("الفكرة\n- ", appendEditorAction("الفكرة", "- "))
    }

    @Test
    fun preservesSingleNewlineBeforeDivider() {
        assertEquals("الفكرة\n---\n", appendEditorAction("الفكرة\n", "---\n"))
    }
    @Test
    fun insertsAtCursorLineAndPreservesArabicSelection() {
        val value = androidx.compose.ui.text.input.TextFieldValue(
            "أول\nفكرة\nآخر", androidx.compose.ui.text.TextRange(4, 8),
        )
        val result = insertEditorAction(value, "- ")
        assertEquals("أول\n- فكرة\nآخر", result.text)
        assertEquals(androidx.compose.ui.text.TextRange(6, 10), result.selection)
    }

    @Test
    fun insertsBeforeFirstLineAtStart() {
        val result = insertEditorAction(androidx.compose.ui.text.input.TextFieldValue("Idea"), "# ")
        assertEquals("# Idea", result.text)
        assertEquals(androidx.compose.ui.text.TextRange(2), result.selection)
    }
}
