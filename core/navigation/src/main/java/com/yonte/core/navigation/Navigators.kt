package com.yonte.core.navigation

interface NotesNavigator {
    fun openNotes()
    fun openNoteEditor(noteId: String? = null, initialText: String? = null)
}

interface HabitsNavigator {
    fun openHabits()
}

interface FinanceNavigator {
    fun openFinance()
}
