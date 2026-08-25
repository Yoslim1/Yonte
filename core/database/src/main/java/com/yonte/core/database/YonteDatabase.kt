package com.yonte.core.database

import android.content.Context
import android.database.SQLException
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [NoteEntity::class], version = 1, exportSchema = true)
abstract class YonteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile private var instance: YonteDatabase? = null

        /** The passphrase-derived key is required; there is deliberately no plaintext
         * fallback. Callers must only reach here after onboarding/unlock completed
         * (enforced by gating injection in MainActivity, not by a default key). */
        fun get(context: Context, passphraseKey: ByteArray): YonteDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext, passphraseKey).also { instance = it }
            }

        private fun build(context: Context, passphraseKey: ByteArray): YonteDatabase {
            System.loadLibrary("sqlcipher")
            return Room.databaseBuilder(context, YonteDatabase::class.java, "yonte.db")
                .openHelperFactory(SupportOpenHelperFactory(passphraseKey))
                .addCallback(object : Callback() {
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
                })
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }
    }
}
