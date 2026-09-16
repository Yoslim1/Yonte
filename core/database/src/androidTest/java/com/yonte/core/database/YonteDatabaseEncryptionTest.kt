package com.yonte.core.database

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.SecureRandom
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/** Proves the on-disk database is genuinely encrypted: it opens with the derived
 * key and the same file is unreadable with any other key. Runs only as an
 * instrumented test (requires SQLCipher natives); CI covers compile + JVM tests. */
@RunWith(AndroidJUnit4::class)
class YonteDatabaseEncryptionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "yonte-encryption-test.db"

    private fun open(key: ByteArray): YonteDatabase =
        Room.databaseBuilder(context, YonteDatabase::class.java, dbName)
            .openHelperFactory(SupportOpenHelperFactory(key))
            .build()

    @Test
    fun singletonCanBeClosedAndReopenedWithTheSameKey() = runBlocking {
        System.loadLibrary("sqlcipher")
        context.deleteDatabase("yonte.db")
        YonteDatabase.close()
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }

        val first = YonteDatabase.get(context, key)
        first.noteDao().getAll()
        YonteDatabase.close()
        val reopened = YonteDatabase.get(context, key)
        reopened.noteDao().getAll()

        assertEquals(false, first.isOpen)
        assertEquals(true, reopened.isOpen)
        YonteDatabase.close()
        context.deleteDatabase("yonte.db")
        Unit
    }

    @Test
    fun freshEncryptedDatabaseCreatesManualFtsTableWhenFts5IsAvailable() {
        System.loadLibrary("sqlcipher")
        YonteDatabase.close()
        context.deleteDatabase("yonte.db")
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }

        try {
            val database = YonteDatabase.get(context, key)
            val sqliteDatabase = database.openHelper.writableDatabase

            assertEquals(
                "A fresh database must create notes_fts exactly when FTS5 is available",
                sqliteDatabase.supportsFts5(),
                sqliteDatabase.hasManualNotesFtsTable(),
            )
        } finally {
            YonteDatabase.close()
            context.deleteDatabase("yonte.db")
        }
    }

    @Test
    fun encryptedDatabaseOpensWithCorrectKeyAndRejectsWrongKey() = runBlocking {
        System.loadLibrary("sqlcipher")
        context.deleteDatabase(dbName)
        val correctKey = ByteArray(32).also { SecureRandom().nextBytes(it) }

        val db = open(correctKey)
        val note = NoteEntity(
            id = "encryption-proof-1",
            title = "سر",
            body = "secret body",
            normalizedText = "سر secret body",
            createdAt = 1_700_000_000_000,
            updatedAt = 1_700_000_000_000,
        )
        db.noteDao().upsert(note)
        assertEquals(note.id, db.noteDao().getById(note.id)?.id)
        db.close()

        try {
            val wrongDb = open(ByteArray(32).also { SecureRandom().nextBytes(it) })
            wrongDb.noteDao().getAll()
            fail("Opening an encrypted database with a wrong key must throw")
        } catch (_: SQLiteException) {
            // Expected: SQLCipher refuses a file decrypted to garbage.
        } finally {
            context.deleteDatabase(dbName)
        }
    }

    private fun SupportSQLiteDatabase.supportsFts5(): Boolean =
        try {
            execSQL("CREATE VIRTUAL TABLE temp.yonte_fts5_capability_check USING fts5(content)")
            true
        } catch (_: SQLException) {
            false
        } finally {
            try {
                execSQL("DROP TABLE IF EXISTS temp.yonte_fts5_capability_check")
            } catch (_: SQLException) {
                // The probe must not turn unsupported FTS5 into a test cleanup failure.
            }
        }

    private fun SupportSQLiteDatabase.hasManualNotesFtsTable(): Boolean =
        query(
            "SELECT 1 FROM sqlite_master WHERE type = ? AND name = ?",
            arrayOf("table", "notes_fts"),
        ).use { cursor ->
            cursor.moveToFirst()
        }
}
