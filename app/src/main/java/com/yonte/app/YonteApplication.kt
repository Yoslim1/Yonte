package com.yonte.app

import android.app.Application
import com.yonte.core.database.YonteDatabase
import com.yonte.core.database.NoteRepository

class YonteApplication : Application() {
    val database: YonteDatabase by lazy { YonteDatabase.get(this) }
    val noteRepository: NoteRepository by lazy { NoteRepository(database) }
}
