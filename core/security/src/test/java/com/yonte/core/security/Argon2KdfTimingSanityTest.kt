package com.yonte.core.security

import java.security.MessageDigest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sanity check that the lowered Argon2 cost parameters did not alter the key contract:
 * a 32-byte key is still produced and the output stays salt-sensitive. No wall-clock
 * bound is asserted here — the CI JVM runner is not representative of the target device,
 * so timing verification is done by the human on real hardware (TASK 8).
 */
class Argon2KdfTimingSanityTest {

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
    fun `deriveNewKey produces a 32-byte key after cost change`() {
        installDeterministicEngine()
        val derived = Argon2Kdf.deriveNewKey("yonte".toCharArray())
        assertEquals(32, derived.key.size)
    }

    @Test
    fun `deriveWithSalt produces a 32-byte key after cost change`() {
        installDeterministicEngine()
        val salt = ByteArray(16) { 3 }
        assertEquals(32, Argon2Kdf.deriveWithSalt("yonte".toCharArray(), salt).size)
    }

    @Test
    fun `key remains salt-sensitive after cost change`() {
        installDeterministicEngine()
        val passphrase = "correct horse battery staple".toCharArray()
        val keyA = Argon2Kdf.deriveWithSalt(passphrase, ByteArray(16) { 0 })
        val keyB = Argon2Kdf.deriveWithSalt(passphrase, ByteArray(16) { 1 })
        assertEquals(32, keyA.size)
        assertEquals(32, keyB.size)
        assertFalse(keyA.contentEquals(keyB))
    }

    @Test
    fun `key remains passphrase-sensitive after cost change`() {
        installDeterministicEngine()
        val salt = ByteArray(16) { 9 }
        val keyA = Argon2Kdf.deriveWithSalt("one".toCharArray(), salt)
        val keyB = Argon2Kdf.deriveWithSalt("two".toCharArray(), salt)
        assertEquals(32, keyA.size)
        assertEquals(32, keyB.size)
        assertFalse(keyA.contentEquals(keyB))
    }

    @Test
    fun `derived key is reproducible for the same passphrase and salt`() {
        installDeterministicEngine()
        val passphrase = "correct horse battery staple".toCharArray()
        val salt = ByteArray(16) { 4 }
        assertTrue(
            Argon2Kdf.deriveWithSalt(passphrase, salt)
                .contentEquals(Argon2Kdf.deriveWithSalt(passphrase, salt)),
        )
    }
}
