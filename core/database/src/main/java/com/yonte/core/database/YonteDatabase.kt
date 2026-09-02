package com.yonte.core.database

import android.content.Context
import android.database.SQLException
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.MessageDigest

@Database(entities = [NoteEntity::class], version = 1, exportSchema = true)
abstract class YonteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile private var instance: YonteDatabase? = null
        private var instanceKeyDigest: ByteArray? = null

        /** The passphrase-derived key is required; there is deliberately no plaintext
         * fallback. Callers must only reach here after onboarding/unlock completed
         * (enforced by gating injection in MainActivity, not by a default key). */
        fun get(context: Context, passphraseKey: ByteArray): YonteDatabase = synchronized(this) {
            val keyDigest = digest(passphraseKey)
            val current = instance
            if (current != null && current.isOpen && instanceKeyDigest?.contentEquals(keyDigest) == true) {
                return current
            }

            current?.close()
            val database = build(context.applicationContext, passphraseKey.copyOf())
            instance = database
            instanceKeyDigest = keyDigest
            database
        }

        /** Closes and forgets the process-local instance, if one exists. */
        fun close() = synchronized(this) {
            instance?.close()
            instance = null
            instanceKeyDigest = null
        }

        private fun digest(key: ByteArray): ByteArray =
            MessageDigest.getInstance("SHA-256").digest(key)

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
                .build()
        }
    }
}
