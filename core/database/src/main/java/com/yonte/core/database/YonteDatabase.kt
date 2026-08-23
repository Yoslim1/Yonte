package com.yonte.core.database

import android.content.Context
import android.database.SQLException
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [NoteEntity::class], version = 1, exportSchema = true)
abstract class YonteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile private var instance: YonteDatabase? = null

        fun get(context: Context): YonteDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                YonteDatabase::class.java,
                "yonte.db",
            ).addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // FTS5 is attempted first. SearchRepository falls back to normalized LIKE
                    // if a vendor SQLite build does not expose the FTS5 module.
                    try {
                        db.execSQL("""
                            CREATE VIRTUAL TABLE IF NOT EXISTS notes_fts USING fts5(
                                note_id UNINDEXED,
                                title,
                                body,
                                tokenize='unicode61'
                            )
                        """.trimIndent())
                    } catch (_: SQLException) {
                        // Capability is detected by SearchRepository; core notes remain usable.
                    }
                }
            }).fallbackToDestructiveMigrationOnDowngrade().build().also { instance = it }
        }
    }
}
