package com.yonte.core.security

import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalKeyManagerTest {

    private lateinit var keyManager: LocalKeyManager

    @Before
    fun setUp() {
        Argon2Kdf.kdfEngine = { password, salt ->
            java.security.MessageDigest.getInstance("SHA-256").apply {
                update(salt)
                update(password)
            }.digest()
        }
        keyManager = LocalKeyManager(ApplicationProvider.getApplicationContext(), XorSessionKeyCipher())
    }

    @After
    fun tearDown() {
        Argon2Kdf.kdfEngine = Argon2Kdf.defaultKdfEngine
    }

    /** Stand-in for the AndroidKeyStore-backed EncryptionManager: a reversible wrap. */
    private class XorSessionKeyCipher : SessionKeyCipher {
        private val mask = ByteArray(32) { (it * 7 + 11).toByte() }
        override fun encrypt(plain: ByteArray): ByteArray =
            ByteArray(plain.size) { i -> (plain[i].toInt() xor mask[i % mask.size].toInt()).toByte() }
        override fun decrypt(payload: ByteArray): ByteArray = encrypt(payload)
    }

    @Test
    fun `isFirstRun is true before setupPassphrase and false after`() {
        assertTrue(keyManager.isFirstRun())
        keyManager.setupPassphrase("first-run-passphrase".toCharArray())
        assertFalse(keyManager.isFirstRun())
    }

    @Test
    fun `unlock with correct passphrase reproduces the setup key`() {
        val setupKey = keyManager.setupPassphrase("daily-driver".toCharArray())
        assertEquals(32, setupKey.size)
        assertArrayEquals(setupKey, keyManager.unlock("daily-driver".toCharArray()))
    }

    @Test(expected = IllegalStateException::class)
    fun `unlock before setupPassphrase throws`() {
        keyManager.unlock("too-early".toCharArray())
    }

    @Test
    fun `unlock with wrong passphrase derives a different key`() {
        val setupKey = keyManager.setupPassphrase("right-passphrase".toCharArray())
        val wrongKey = keyManager.unlock("wrong-passphrase".toCharArray())
        assertFalse(setupKey.contentEquals(wrongKey))
    }

    @Test
    fun `session cache round-trips through the cipher across manager instances`() {
        assertNull(keyManager.cachedSessionKey())
        val setupKey = keyManager.setupPassphrase("persisted-unlock".toCharArray())
        val reopened = LocalKeyManager(ApplicationProvider.getApplicationContext(), XorSessionKeyCipher())
        assertArrayEquals(setupKey, reopened.cachedSessionKey())
    }
}
