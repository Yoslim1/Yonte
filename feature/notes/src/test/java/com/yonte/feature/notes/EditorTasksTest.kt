package com.yonte.feature.notes

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditorTasksTest {
    @Test
    fun mixedArabicTasksToggleWithoutChangingTextOrLineEndings() {
        val original = "مقدمة\r\n- [ ] شراء الكتاب\r\n* [X] قراءة\r\nخاتمة"
        val value = TextFieldValue(original, TextRange(original.length))
        val blocks = editorBlocks(original)
        assertEquals(listOf("مقدمة", "شراء الكتاب", "قراءة", "خاتمة"),
            blocks.map { original.substring(it.contentStart, it.end) })
        val checked = toggleEditorTask(value, blocks[1])
        assertEquals(original.replace("[ ]", "[x]"), checked.text)
        assertEquals(value.selection, checked.selection)
        assertEquals(original, toggleEditorTask(checked, editorBlocks(checked.text)[1]).text)
    }

    @Test
    fun fencedCodeAndOrdinaryBracketsStayText() {
        val text = "```md\n- [ ] code\n```\n~~~\n- [x] also code\n~~~\n[ ] prose\n- [ ] task"
        val tasks = editorBlocks(text).filter { it.checkOffset != null }
        assertEquals(1, tasks.size)
        assertEquals("task", text.substring(tasks.single().contentStart, tasks.single().end))
    }

    @Test
    fun taskEditPreservesSurroundingDocumentAndComposition() {
        val value = TextFieldValue("قبل\r\n- [ ] نص\r\nبعد")
        val block = editorBlocks(value.text)[1]
        val replacement = TextFieldValue("نص جديد", TextRange(7), TextRange(3, 7))
        val updated = replaceEditorBlock(value, block, replacement)
        assertEquals("قبل\r\n- [ ] نص جديد\r\nبعد", updated.text)
        assertEquals(replacement, editorBlockValue(updated, editorBlocks(updated.text)[1]))
    }

    @Test
    fun enterContinuesTaskAtCursorAndPreservesIndentationAndCrLf() {
        val value = TextFieldValue("قبل\r\n  - [x] firstsecond\r\nبعد")
        val block = editorBlocks(value.text)[1]
        val updated = replaceEditorBlock(value, block, TextFieldValue("first\nsecond", TextRange(6)))
        assertEquals("قبل\r\n  - [x] first\r\n  - [ ] second\r\nبعد", updated.text)
        assertEquals(updated.text.indexOf("second"), updated.selection.start)
        assertNull(updated.composition)
    }

    @Test
    fun enterOnEmptyTaskExitsList() {
        val value = TextFieldValue("- [ ] first\n- [ ] ")
        val updated = replaceEditorBlock(value, editorBlocks(value.text).last(), TextFieldValue("\n", TextRange(1)))
        assertEquals("- [ ] first\n", updated.text)
        assertEquals(TextRange(updated.text.length), updated.selection)
        assertNull(editorBlocks(updated.text).last().checkOffset)
    }

    @Test
    fun multilinePasteIsNotMistakenForEnter() {
        val value = TextFieldValue("- [ ] old")
        val updated = replaceEditorBlock(value, editorBlocks(value.text).single(), TextFieldValue("one\ntwo", TextRange(7)))
        assertEquals("- [ ] one\ntwo", updated.text)
    }

    @Test
    fun taskActionOnExistingTaskAddsNewTaskAndPreservesCrLf() {
        val value = TextFieldValue("- [ ] أول\r\nآخر", TextRange(8))
        val updated = insertEditorAction(value, "- [ ] ")
        assertEquals("- [ ] أول\r\n- [ ] \r\nآخر", updated.text)
        assertEquals(TextRange(17), updated.selection)
    }

    @Test
    fun taskActionAfterFinalTaskPreservesDocumentCrLf() {
        val text = "intro\r\n- [ ] last"
        val updated = insertEditorAction(TextFieldValue(text, TextRange(text.length)), "- [ ] ")
        assertEquals("intro\r\n- [ ] last\r\n- [ ] ", updated.text)
        assertEquals(TextRange(updated.text.length), updated.selection)
    }

    @Test
    fun emptyAndTrailingLinesRemainEditable() {
        assertEquals(listOf(EditorBlock(0, 0)), editorBlocks(""))
        val text = "- [ ] done\r\n"
        assertEquals(EditorBlock(text.length, text.length), editorBlocks(text).last())
    }
}
