package com.yonte.core.security

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** JVM-only fake [BiometricCipherProvider] backed by a hardcoded AES/GCM key. */
private class FakeCipherProvider : BiometricCipherProvider {
    private val key: SecretKey = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")

    override fun encryptCipher(): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, ByteArray(12) { it.toByte() }))
        }

    override fun decryptCipher(iv: ByteArray): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        }
}

@RunWith(RobolectricTestRunner::class)
class BiometricUnlockManagerTest {

    private lateinit var manager: BiometricUnlockManager

    @Before
    fun setUp() {
        val provider = FakeCipherProvider()
        manager = BiometricUnlockManager(RuntimeEnvironment.getApplication(), provider)
        // Ensure clean state — clear any leftover data from previous tests
        manager.clearEnrolledKey()
    }

    @Test
    fun `hasEnrolledKey returns false initially`() {
        assertFalse(manager.hasEnrolledKey())
    }

    @Test
    fun `persist and unwrap round-trip`() {
        val sessionKey = ByteArray(32) { (it + 10).toByte() }

        val encryptCipher = manager.buildEncryptCipher()
        manager.persistEncryptedKey(encryptCipher, sessionKey)

        assertTrue(manager.hasEnrolledKey())

        val decryptCipher = manager.buildDecryptCipher()
        assertNotNull(decryptCipher)
        val decrypted = manager.unwrapSessionKey(decryptCipher!!)

        assertNotNull(decrypted)
        assertEquals(sessionKey.toList(), decrypted!!.toList())
    }

    @Test
    fun `buildDecryptCipher returns null when no key enrolled`() {
        assertNull(manager.buildDecryptCipher())
    }

    @Test
    fun `clearEnrolledKey removes stored data`() {
        val sessionKey = ByteArray(32) { it.toByte() }
        val encryptCipher = manager.buildEncryptCipher()
        manager.persistEncryptedKey(encryptCipher, sessionKey)

        assertTrue(manager.hasEnrolledKey())

        manager.clearEnrolledKey()

        assertFalse(manager.hasEnrolledKey())
        assertNull(manager.buildDecryptCipher())
    }

    @Test(expected = IllegalStateException::class)
    fun `unwrapSessionKey throws when cache_data is missing`() {
        // Write a valid IV but no cache_data
        val encryptCipher = manager.buildEncryptCipher()
        val prefs = RuntimeEnvironment.getApplication()
            .getSharedPreferences("yonte_biometric_cache", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("cache_iv", android.util.Base64.encodeToString(encryptCipher.iv, android.util.Base64.NO_WRAP))
            .apply()

        val decryptCipher = manager.buildDecryptCipher()
        assertNotNull(decryptCipher)
        manager.unwrapSessionKey(decryptCipher!!)
    }
}
