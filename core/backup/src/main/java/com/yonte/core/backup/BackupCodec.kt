package com.yonte.core.backup

import com.yonte.core.security.Argon2Kdf
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Portable backup encryption with its own passphrase, deliberately independent from
 * the local unlock passphrase: a backup can be shared under different credentials
 * than the daily device unlock. Wire format: [saltLen][salt][ivLen][iv][ciphertext]. */
class BackupCodec {
    fun encrypt(plain: ByteArray, passphrase: CharArray): ByteArray {
        val derived = Argon2Kdf.deriveNewKey(passphrase)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(derived.key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain)
        return ByteBuffer.allocate(4 + derived.salt.size + 4 + iv.size + ciphertext.size).apply {
            putInt(derived.salt.size); put(derived.salt)
            putInt(iv.size); put(iv)
            put(ciphertext)
        }.array()
    }

    fun decrypt(payload: ByteArray, passphrase: CharArray): ByteArray {
        val buffer = ByteBuffer.wrap(payload)
        val salt = ByteArray(buffer.int).also(buffer::get)
        val iv = ByteArray(buffer.int).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        val key = Argon2Kdf.deriveWithSalt(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    /** Encrypt with a pre-derived key and explicit salt. Produces the same wire
     * format as [encrypt] so import needs no changes. */
    fun encryptWithKey(plain: ByteArray, key: ByteArray, salt: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain)
        return ByteBuffer.allocate(4 + salt.size + 4 + iv.size + ciphertext.size).apply {
            putInt(salt.size); put(salt)
            putInt(iv.size); put(iv)
            put(ciphertext)
        }.array()
    }
}
