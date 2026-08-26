package com.yonte.feature.settings

import android.content.ContentResolver
import android.net.Uri
import com.yonte.core.backup.BackupGateway
import com.yonte.core.backup.BackupNote
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupGatewayWiringTest {

    private class RecordingGateway : BackupGateway {
        var legacyExportCalls = 0
        var legacyImportCalls = 0
        var secureExportCalls = 0
        var secureImportCalls = 0
        var lastSecurePassphrase: CharArray? = null

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
    }

    @Test
    fun `export goes through the passphrase overload exactly once and never the legacy one`() {
        val gateway = RecordingGateway()
        val notes = listOf(sampleNote("n1"))
        val passphrase = "correct horse battery staple".toCharArray()

        PassphraseBackupFlow.export(gateway, NULL_RESOLVER, NULL_URI, notes, passphrase)

        assertEquals(1, gateway.secureExportCalls)
        assertEquals(0, gateway.legacyExportCalls)
        assertEquals(0, gateway.legacyImportCalls)
        assertArrayEquals(passphrase, gateway.lastSecurePassphrase)
    }

    @Test
    fun `import goes through the passphrase overload exactly once and never the legacy one`() {
        val gateway = RecordingGateway()
        val passphrase = "another-passphrase".toCharArray()

        val restored = PassphraseBackupFlow.import(gateway, NULL_RESOLVER, NULL_URI, passphrase)

        assertEquals(1, gateway.secureImportCalls)
        assertEquals(0, gateway.legacyImportCalls)
        assertEquals(0, gateway.legacyExportCalls)
        assertEquals(listOf(sampleNote("restored")), restored)
        assertArrayEquals(passphrase, gateway.lastSecurePassphrase)
    }

    private companion object {
        private val NULL_RESOLVER: ContentResolver = null as ContentResolver
        private val NULL_URI: Uri = null as Uri

        private fun sampleNote(id: String) = BackupNote(id, "title", "body", isPinned = true, createdAt = 1L, updatedAt = 2L)
    }
}
