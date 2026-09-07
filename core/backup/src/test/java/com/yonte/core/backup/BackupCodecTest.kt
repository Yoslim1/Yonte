package com.yonte.core.backup

import com.yonte.core.security.Argon2Kdf
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.MessageDigest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/** AES/GCM round-trip is exercised on the JVM with a deterministic KDF substitute
 * (argon2kt natives are Android-only); the real-Argon2 path is covered by the
 * instrumented database test. */
class BackupCodecTest {

    @Before
    fun setUp() {
        Argon2Kdf.installKdfEngineForTesting { password, salt ->
            MessageDigest.getInstance("SHA-256").apply {
                update(salt)
                update(password)
            }.digest()
        }
    }

    @After
    fun tearDown() {
        Argon2Kdf.installKdfEngineForTesting(null)
    }

    private val codec = BackupCodec()
    private val payload = "ynote-backup-payload".toByteArray(Charsets.UTF_8)

    @Test
    fun `encrypt then decrypt with correct passphrase round-trips`() {
        val encrypted = codec.encrypt(payload, "backup-pass".toCharArray())
        assertFalse(encrypted.contentEquals(payload))
        assertArrayEquals(payload, codec.decrypt(encrypted, "backup-pass".toCharArray()))
    }

    @Test(expected = GeneralSecurityException::class)
    fun `decrypt with wrong passphrase throws`() {
        val encrypted = codec.encrypt(payload, "backup-pass".toCharArray())
        codec.decrypt(encrypted, "wrong-pass".toCharArray())
    }

    @Test
    fun `same passphrase produces fresh salt and distinct ciphertexts`() {
        val first = codec.encrypt(payload, "stable-pass".toCharArray())
        val second = codec.encrypt(payload, "stable-pass".toCharArray())
        assertFalse(first.contentEquals(second))
        assertArrayEquals(payload, codec.decrypt(first, "stable-pass".toCharArray()))
        assertArrayEquals(payload, codec.decrypt(second, "stable-pass".toCharArray()))
    }

    @Test(expected = GeneralSecurityException::class)
    fun `tampered ciphertext fails authentication`() {
        val encrypted = codec.encrypt(payload, "tamper-check".toCharArray())
        encrypted[encrypted.size - 1] = (encrypted[encrypted.size - 1].toInt() xor 0x01).toByte()
        codec.decrypt(encrypted, "tamper-check".toCharArray())
    }

    @Test
    fun `empty plaintext round-trips`() {
        val encrypted = codec.encrypt(ByteArray(0), "empty-body".toCharArray())
        assertArrayEquals(ByteArray(0), codec.decrypt(encrypted, "empty-body".toCharArray()))
    }

    @Test
    fun `malformed headers are rejected before key derivation`() {
        val encrypted = codec.encrypt(payload, "backup-pass".toCharArray())
        Argon2Kdf.installKdfEngineForTesting { _, _ -> error("Malformed input reached KDF") }
        for (length in listOf(Int.MIN_VALUE, -1, 0, 15, 17, Int.MAX_VALUE)) {
            val malformed = encrypted.copyOf()
            ByteBuffer.wrap(malformed).putInt(0, length)
            assertThrows(IllegalArgumentException::class.java) {
                codec.decrypt(malformed, charArrayOf())
            }
        }
        for (length in listOf(Int.MIN_VALUE, -1, 0, 11, 13, Int.MAX_VALUE)) {
            val malformed = encrypted.copyOf()
            ByteBuffer.wrap(malformed).putInt(20, length)
            assertThrows(IllegalArgumentException::class.java) {
                codec.decrypt(malformed, charArrayOf())
            }
        }
        Argon2Kdf.installKdfEngineForTesting(null)
    }

    @Test
    fun `oversized ciphertext rejected before key derivation`() {
        Argon2Kdf.installKdfEngineForTesting { _, _ -> error("Oversized input reached KDF") }
        assertThrows(IllegalArgumentException::class.java) {
            codec.decrypt(ByteArray(MAX_BACKUP_FILE_BYTES + 1), charArrayOf())
        }
        Argon2Kdf.installKdfEngineForTesting(null)
    }
}
