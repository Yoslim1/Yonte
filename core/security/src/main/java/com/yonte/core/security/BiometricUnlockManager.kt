package com.yonte.core.security

import android.content.Context
import android.util.Base64
import javax.crypto.Cipher

/**
 * Manages biometric-gated session-key wrap/unwrap and persists the encrypted
 * key material in SharedPreferences.  Extracted from [MainActivity] so that
 * the biometric cache storage is reusable and testable.
 *
 * @param context  Application context — never holds an Activity reference.
 * @param cipherProvider  Abstraction over AndroidKeyStore-backed ciphers.
 *                        Production: [BiometricGateCipher].  Tests: JVM fake.
 */
class BiometricUnlockManager(
    context: Context,
    private val cipherProvider: BiometricCipherProvider,
) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns true if [cache_iv] and [cache_data] are both present. */
    fun hasEnrolledKey(): Boolean =
        prefs.getString(KEY_IV, null) != null && prefs.getString(KEY_DATA, null) != null

    /** Creates an encrypt cipher for wrapping the session key. */
    fun buildEncryptCipher(): Cipher = cipherProvider.encryptCipher()

    /** Wraps [sessionKey] with the given [cipher], persists the IV and ciphertext, and
     *  returns the encrypted result. Callers must zero [sessionKey] after calling. */
    fun persistEncryptedKey(cipher: Cipher, sessionKey: ByteArray): ByteArray {
        val encrypted = cipher.doFinal(sessionKey)
        prefs.edit()
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_DATA, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
        return encrypted
    }

    /** Builds a decrypt cipher from the persisted IV. Returns null if no IV is stored. */
    fun buildDecryptCipher(): Cipher? {
        val ivB64 = prefs.getString(KEY_IV, null) ?: return null
        val iv = Base64.decode(ivB64, Base64.NO_WRAP)
        return cipherProvider.decryptCipher(iv)
    }

    /** Decrypts and returns the persisted session key.
     *  @throws IllegalStateException if no cache data is stored. */
    fun unwrapSessionKey(cipher: Cipher): ByteArray {
        val dataB64 = prefs.getString(KEY_DATA, null)
            ?: error("No cached biometric session key — call hasEnrolledKey() first")
        val encryptedData = Base64.decode(dataB64, Base64.NO_WRAP)
        return cipher.doFinal(encryptedData)
    }

    /** Removes the persisted biometric key cache entirely. */
    fun clearEnrolledKey() {
        prefs.edit().remove(KEY_IV).remove(KEY_DATA).apply()
    }

    private companion object {
        const val PREFS_NAME = "yonte_biometric_cache"
        const val KEY_IV = "cache_iv"
        const val KEY_DATA = "cache_data"
    }
}
