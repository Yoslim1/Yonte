package com.yonte.core.backup

import com.yonte.core.security.Argon2Kdf
import java.security.MessageDigest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/** Proves that encryptWithKey (raw key + explicit salt) produces the same wire
 * format as encrypt (passphrase-based), so import with a passphrase that derives
 * the same key from the same salt will successfully decrypt the export. */
class BackupCodecKeyRoundTripTest {

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
    private val payload = "ynote-backup-roundtrip".toByteArray(Charsets.UTF_8)

    @Test
    fun `encryptWithKey then decrypt with matching passphrase round-trips`() {
        val passphrase = "test-passphrase".toCharArray()
        val salt = ByteArray(16) { it.toByte() }
        val key = Argon2Kdf.deriveWithSalt(passphrase, salt)

        val encrypted = codec.encryptWithKey(payload, key, salt)
        assertFalse(encrypted.contentEquals(payload))

        // decrypt uses the passphrase; the KDF will re-derive the same key from the embedded salt
        val decrypted = codec.decrypt(encrypted, passphrase)
        assertArrayEquals(payload, decrypted)
    }

    @Test
    fun `encryptWithKey produces same wire format as encrypt`() {
        val passphrase = "stable-pass".toCharArray()
        val salt = ByteArray(16) { 42 }

        // Manually derive the key the same way encrypt() would
        val key = Argon2Kdf.deriveWithSalt(passphrase, salt)

        // encryptWithKey with the pre-derived key
        val keyBased = codec.encryptWithKey(payload, key, salt)

        // encrypt with the passphrase (generates a different random salt internally)
        val passphraseBased = codec.encrypt(payload, passphrase)

        // Both should decrypt with the correct passphrase
        assertArrayEquals(payload, codec.decrypt(keyBased, passphrase))
        assertArrayEquals(payload, codec.decrypt(passphraseBased, passphrase))
    }
}
