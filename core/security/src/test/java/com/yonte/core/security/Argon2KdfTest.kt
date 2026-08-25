package com.yonte.core.security

import java.security.MessageDigest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs the KDF contract against a deterministic substitute engine because argon2kt's
 * native library only loads on Android; real-Argon2 behavior is covered end-to-end by
 * the instrumented database test (correct key opens, wrong key throws).
 */
class Argon2KdfTest {

    @After
    fun restoreRealEngine() {
        Argon2Kdf.installKdfEngineForTesting(null)
    }

    private fun installDeterministicEngine() {
        Argon2Kdf.installKdfEngineForTesting { password, salt ->
            MessageDigest.getInstance("SHA-256").apply {
                update(salt)
                update(password)
            }.digest()
        }
    }

    @Test
    fun `same passphrase and same salt derive identical key`() {
        installDeterministicEngine()
        val passphrase = "correct horse battery staple".toCharArray()
        val salt = ByteArray(16) { it.toByte() }
        assertArrayEquals(
            Argon2Kdf.deriveWithSalt(passphrase, salt),
            Argon2Kdf.deriveWithSalt(passphrase, salt),
        )
    }

    @Test
    fun `same passphrase with different salts derives different keys`() {
        installDeterministicEngine()
        val passphrase = "correct horse battery staple".toCharArray()
        assertNotEquals(
            Argon2Kdf.deriveWithSalt(passphrase, ByteArray(16) { 0 }),
            Argon2Kdf.deriveWithSalt(passphrase, ByteArray(16) { 1 }),
        )
    }

    @Test
    fun `different passphrases with same salt derive different keys`() {
        installDeterministicEngine()
        val salt = ByteArray(16) { 7 }
        assertNotEquals(
            Argon2Kdf.deriveWithSalt("passphrase-one".toCharArray(), salt),
            Argon2Kdf.deriveWithSalt("passphrase-two".toCharArray(), salt),
        )
    }

    @Test
    fun `deriveNewKey returns fresh 16-byte salt and 32-byte key every call`() {
        installDeterministicEngine()
        val first = Argon2Kdf.deriveNewKey("yonte".toCharArray())
        val second = Argon2Kdf.deriveNewKey("yonte".toCharArray())
        assertEquals(16, first.salt.size)
        assertEquals(32, first.key.size)
        assertFalse(first.salt.contentEquals(second.salt))
        assertTrue(first.key.contentEquals(Argon2Kdf.deriveWithSalt("yonte".toCharArray(), first.salt)))
    }
}
