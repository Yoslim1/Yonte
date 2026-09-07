package com.yonte.core.backup

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupPayloadCompatibilityTest {
    @Test
    fun `archive and trash flags survive serialization`() {
        val note = BackupNote("id", "عنوان", "body", true, 1L, 2L, true, true)
        val root = JSONObject(buildNotesPayload(listOf(note)).toString(Charsets.UTF_8))
        assertEquals(note, parseBackupNote(root.getJSONArray("notes").getJSONObject(0)))
    }

    @Test
    fun `legacy records default to active without losing contents`() {
        val note = BackupNote("id", "title", "body", false, 1L, 2L)
        val root = JSONObject(buildNotesPayload(listOf(note)).toString(Charsets.UTF_8))
        val item = root.getJSONArray("notes").getJSONObject(0)
        item.remove("is_archived")
        item.remove("is_trashed")
        assertEquals(note, parseBackupNote(item))
    }

    @Test
    fun `export rejects envelopes the importer cannot accept`() {
        assertThrows(IllegalArgumentException::class.java) {
            buildEncryptedEnvelope(byteArrayOf(1)) { ByteArray(MAX_BACKUP_FILE_BYTES) }
        }
    }
}
