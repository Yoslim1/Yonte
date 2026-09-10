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
        fun get(
            context: Context,
            passphraseKey: ByteArray,
            isAuthorized: (() -> Boolean)? = null,
        ): YonteDatabase = synchronized(this) {
            check(isAuthorized?.invoke() != false) { "Database access is no longer authorized" }
            val keyDigest = digest(passphraseKey)
            val current = instance
            if (current != null && current.isOpen && instanceKeyDigest?.contentEquals(keyDigest) == true) {
                return current
            }

            // Open the replacement before touching the current singleton. A wrong
            // direct candidate must leave an already-valid database available.
            val database = build(context.applicationContext, passphraseKey.copyOf())
            try {
                database.openHelper.writableDatabase
            } catch (e: Exception) {
                database.close()
                throw e
            }

            current?.close()
            instance = database
            instanceKeyDigest = keyDigest
            database
        }

        /** Opens a candidate key for protected-storage validation without touching the
         * process-level singleton. A wrong candidate therefore cannot close or poison
         * an already authenticated database instance. The caller owns and closes the
         * returned database after its validation query. */
        fun openForValidation(context: Context, passphraseKey: ByteArray): YonteDatabase {
            val database = build(context.applicationContext, passphraseKey.copyOf())
            return try {
                database.openHelper.writableDatabase
                database
            } catch (e: Exception) {
                database.close()
                throw e
            }
        }

        /** Closes and forgets the process-local instance, if one exists. Never throws:
         * safe to call with no instance, an already-closed one, or after a failed open
         * (e.g. a version-mismatch failure that must surface a blocking UI instead). */
        fun close() = synchronized(this) {
            try {
                val current = instance
                if (current != null && current.isOpen) {
                    current.close()
                }
            } catch (_: Exception) {
                // Best-effort cleanup only; a close-path failure must not crash the caller.
            } finally {
                instance = null
                instanceKeyDigest = null
            }
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
