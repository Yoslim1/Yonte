package com.yonte.core.backup

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupImportLimitTest {

    @Test
    fun `readBackupBytes accepts input under the limit`() {
        val bytes = ByteArray(1024) { it.toByte() }
        assertArrayEquals(bytes, readBackupBytes(ByteArrayInputStream(bytes)))
    }

    @Test
    fun `readBackupBytes rejects oversized input`() {
        val oversized = ByteArray(MAX_BACKUP_FILE_BYTES + 1)
        assertThrows(IllegalArgumentException::class.java) {
            readBackupBytes(ByteArrayInputStream(oversized))
        }
    }
}
