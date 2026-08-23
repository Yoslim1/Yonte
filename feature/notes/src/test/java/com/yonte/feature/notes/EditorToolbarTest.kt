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
}
