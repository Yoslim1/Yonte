package com.yonte.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

class LocalKeyManager(context: Context, private val cacheManager: SessionKeyCipher) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("yonte_local_key_meta", Context.MODE_PRIVATE)

    fun isFirstRun(): Boolean = !prefs.contains(KEY_SALT)

    /** Called once, from onboarding. Derives and persists the salt (not the key). */
    fun setupPassphrase(passphrase: CharArray): ByteArray {
        val derived = Argon2Kdf.deriveNewKey(passphrase)
        prefs.edit().putString(KEY_SALT, Base64.encodeToString(derived.salt, Base64.NO_WRAP)).apply()
        cacheSessionKey(derived.key)
        return derived.key
    }

    /** Called on every cold start after onboarding. Re-derives the key from the
     * passphrase the user re-enters (or from the cached wrapped key after a
     * biometric/PIN unlock — wiring for that convenience layer is out of scope for
     * this task and tracked separately in ROADMAP.md). */
    fun unlock(passphrase: CharArray): ByteArray {
        val salt = Base64.decode(
            prefs.getString(KEY_SALT, null) ?: error("setupPassphrase() was never called"),
            Base64.NO_WRAP,
        )
        val key = Argon2Kdf.deriveWithSalt(passphrase, salt)
        cacheSessionKey(key)
        return key
    }

    /** Returns the raw salt bytes, or null before first-run setup. */
    fun currentSalt(): ByteArray? =
        prefs.getString(KEY_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) }

    /** Returns the configured unlock method ("PASSPHRASE", "PIN", or "BIOMETRIC").
     * Defaults to "PASSPHRASE" for existing installs with no preference set. */
    fun unlockMethod(): String = prefs.getString(KEY_UNLOCK_METHOD, METHOD_PASSPHRASE)
        ?: METHOD_PASSPHRASE

    fun setUnlockMethod(method: String) {
        prefs.edit().putString(KEY_UNLOCK_METHOD, method).apply()
    }

    /** Unwraps the persisted session-key cache written by setupPassphrase/unlock.
     * Returns null before onboarding or when the Keystore-wrapped blob can no longer
     * be decrypted; callers must treat null as "not unlocked" instead of falling back
     * to any insecure default. */
    fun cachedSessionKey(): ByteArray? {
        val wrapped = prefs.getString(KEY_SESSION_CACHE, null) ?: return null
        return runCatching { cacheManager.decrypt(Base64.decode(wrapped, Base64.NO_WRAP)) }.getOrNull()
    }

    /** Clears the session-key cache so the next cold start requires re-authentication. */
    fun clearSessionCache() {
        prefs.edit().remove(KEY_SESSION_CACHE).apply()
    }

    /** Directly caches a raw session key (bypasses EncryptionManager wrapping).
     * Used after biometric authentication where the key was already unwrapped from
     * the biometric-bound cipher. */
    fun cacheSessionKeyDirectly(key: ByteArray) {
        prefs.edit().putString(
            KEY_SESSION_CACHE,
            Base64.encodeToString(cacheManager.encrypt(key), Base64.NO_WRAP),
        ).apply()
    }

    private fun cacheSessionKey(key: ByteArray) {
        // Wrapped with the existing AndroidKeyStore-backed EncryptionManager so the
        // raw passphrase-derived key never touches disk in plaintext, even for the
        // in-session convenience cache.
        prefs.edit().putString(
            KEY_SESSION_CACHE,
            Base64.encodeToString(cacheManager.encrypt(key), Base64.NO_WRAP),
        ).apply()
    }

    companion object {
        private const val KEY_SALT = "local_key_salt"
        private const val KEY_SESSION_CACHE = "local_key_session_cache"
        private const val KEY_UNLOCK_METHOD = "unlock_method"
        internal const val METHOD_PASSPHRASE = "PASSPHRASE"
        internal const val METHOD_PIN = "PIN"
        internal const val METHOD_BIOMETRIC = "BIOMETRIC"
    }
}
