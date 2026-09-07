package com.yonte.core.backup

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupImportLimitTest {
    @Test
    fun `empty and exact limit streams retain every byte`() {
        assertArrayEquals(ByteArray(0), readBackupBytes(ByteArrayInputStream(ByteArray(0)), 8))
        val bytes = ByteArray(16) { it.toByte() }
        assertArrayEquals(bytes, readBackupBytes(ByteArrayInputStream(bytes), bytes.size))
    }

    @Test
    fun `short reads do not truncate the envelope`() {
        val bytes = ByteArray(32) { it.toByte() }
        val stream = object : ByteArrayInputStream(bytes) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                super.read(buffer, offset, minOf(length, 3))
        }
        assertArrayEquals(bytes, readBackupBytes(stream, bytes.size))
    }

    @Test
    fun `unbounded stream is rejected after limit plus one bytes`() {
        var reads = 0
        val stream = object : InputStream() {
            override fun read(): Int { reads++; return 1 }
        }
        assertThrows(IllegalArgumentException::class.java) { readBackupBytes(stream, 16) }
        assertEquals(17, reads)
    }

    @Test
    fun `empty bulk reads still progress`() {
        val bytes = byteArrayOf(1, 2, 3)
        val stream = object : ByteArrayInputStream(bytes) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = 0
        }
        assertArrayEquals(bytes, readBackupBytes(stream, 8))
    }

    @Test
    fun `source failure propagates instead of importing partial data`() {
        val stream = object : InputStream() {
            override fun read(): Int = throw IOException("Source disconnected")
        }
        assertThrows(IOException::class.java) { readBackupBytes(stream, 16) }
    }
}
