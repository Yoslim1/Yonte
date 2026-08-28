package com.yonte.feature.settings

import android.content.ContentResolver
import android.net.Uri
import com.yonte.core.backup.BackupGateway
import com.yonte.core.backup.BackupNote
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class BackupGatewayWiringTest {

    private class RecordingGateway : BackupGateway {
        var legacyExportCalls = 0
        var legacyImportCalls = 0
        var secureExportCalls = 0
        var secureImportCalls = 0
        var keyExportCalls = 0
        var lastSecurePassphrase: CharArray? = null
        var lastKeyExportKey: ByteArray? = null
        var lastKeyExportSalt: ByteArray? = null

        override fun exportNotes(resolver: ContentResolver, uri: Uri, notes: List<BackupNote>) {
            legacyExportCalls++
        }

        override fun importNotes(resolver: ContentResolver, uri: Uri): List<BackupNote> {
            legacyImportCalls++
            return emptyList()
        }

        override fun exportNotes(resolver: ContentResolver, uri: Uri, notes: List<BackupNote>, passphrase: CharArray) {
            secureExportCalls++
            lastSecurePassphrase = passphrase.copyOf()
        }

        override fun importNotes(resolver: ContentResolver, uri: Uri, passphrase: CharArray): List<BackupNote> {
            secureImportCalls++
            lastSecurePassphrase = passphrase.copyOf()
            return listOf(sampleNote("restored"))
        }

        override fun exportNotes(resolver: ContentResolver, uri: Uri, notes: List<BackupNote>, key: ByteArray, salt: ByteArray) {
            keyExportCalls++
            lastKeyExportKey = key.copyOf()
            lastKeyExportSalt = salt.copyOf()
        }
    }

    @Test
    fun `export goes through the passphrase overload exactly once and never the legacy one`() {
        val gateway = RecordingGateway()
        val notes = listOf(sampleNote("n1"))
        val passphrase = "correct horse battery staple".toCharArray()

        PassphraseBackupFlow.export(gateway, FAKE_RESOLVER, FAKE_URI, notes, passphrase)

        assertEquals(1, gateway.secureExportCalls)
        assertEquals(0, gateway.legacyExportCalls)
        assertEquals(0, gateway.legacyImportCalls)
        assertArrayEquals(passphrase, gateway.lastSecurePassphrase)
    }

    @Test
    fun `import goes through the passphrase overload exactly once and never the legacy one`() {
        val gateway = RecordingGateway()
        val passphrase = "another-passphrase".toCharArray()

        val restored = PassphraseBackupFlow.import(gateway, FAKE_RESOLVER, FAKE_URI, passphrase)

        assertEquals(1, gateway.secureImportCalls)
        assertEquals(0, gateway.legacyImportCalls)
        assertEquals(0, gateway.legacyExportCalls)
        assertEquals(listOf(sampleNote("restored")), restored)
        assertArrayEquals(passphrase, gateway.lastSecurePassphrase)
    }

    @Test
    fun `exportWithKey goes through the key-based overload exactly once`() {
        val gateway = RecordingGateway()
        val notes = listOf(sampleNote("n1"))
        val key = ByteArray(32) { it.toByte() }
        val salt = ByteArray(16) { (it * 2).toByte() }

        PassphraseBackupFlow.exportWithKey(gateway, FAKE_RESOLVER, FAKE_URI, notes, key, salt)

        assertEquals(1, gateway.keyExportCalls)
        assertEquals(0, gateway.secureExportCalls)
        assertEquals(0, gateway.legacyExportCalls)
        assertEquals(0, gateway.legacyImportCalls)
        assertArrayEquals(key, gateway.lastKeyExportKey)
        assertArrayEquals(salt, gateway.lastKeyExportSalt)
    }

    private companion object {
        // Real (non-null) Mockito doubles instead of unchecked null casts: Kotlin
        // generates a null-check on every non-nullable parameter at the call site,
        // so passing an actual `null` (even via `as T`) throws NullPointerException
        // before the fake gateway is ever reached. RecordingGateway never calls a
        // method on these, so a bare mock with no stubbing is sufficient.
        private val FAKE_RESOLVER: ContentResolver = mock(ContentResolver::class.java)
        private val FAKE_URI: Uri = mock(Uri::class.java)

        private fun sampleNote(id: String) = BackupNote(id, "title", "body", isPinned = true, createdAt = 1L, updatedAt = 2L)
    }
}
