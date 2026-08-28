package com.yonte.core.security

import android.content.Context
import android.util.Base64

class AppPinManager(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("yonte_app_pin", Context.MODE_PRIVATE)

    fun isPinSet(): Boolean = prefs.contains(KEY_HASH)

    fun setPin(pin: CharArray) {
        val derived = Argon2Kdf.deriveNewKey(pin)
        prefs.edit()
            .putString(KEY_HASH, Base64.encodeToString(derived.key, Base64.NO_WRAP))
            .putString(KEY_SALT, Base64.encodeToString(derived.salt, Base64.NO_WRAP))
            .putInt(KEY_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    /** Returns true on success, or false on failure. Callers must check
     * [lockoutSecondsRemaining] before calling verify. */
    fun verify(pin: CharArray): Boolean {
        val salt = Base64.decode(prefs.getString(KEY_SALT, null) ?: return false, Base64.NO_WRAP)
        val expected = Base64.decode(prefs.getString(KEY_HASH, null) ?: return false, Base64.NO_WRAP)
        val actual = Argon2Kdf.deriveWithSalt(pin, salt)
        val matches = actual.contentEquals(expected)
        if (matches) {
            prefs.edit().putInt(KEY_ATTEMPTS, 0).putLong(KEY_LOCKOUT_UNTIL, 0L).apply()
        } else {
            val attempts = prefs.getInt(KEY_ATTEMPTS, 0) + 1
            val lockoutSeconds = if (attempts >= 5) 30L * (1 shl (attempts - 5).coerceAtMost(6)) else 0L
            prefs.edit()
                .putInt(KEY_ATTEMPTS, attempts)
                .putLong(KEY_LOCKOUT_UNTIL, if (lockoutSeconds > 0) System.currentTimeMillis() + lockoutSeconds * 1000 else 0L)
                .apply()
        }
        return matches
    }

    /** 0 if not currently locked out. */
    fun lockoutSecondsRemaining(): Long {
        val until = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val remaining = (until - System.currentTimeMillis()) / 1000
        return remaining.coerceAtLeast(0L)
    }

    private companion object {
        const val KEY_HASH = "pin_hash"
        const val KEY_SALT = "pin_salt"
        const val KEY_ATTEMPTS = "pin_attempts"
        const val KEY_LOCKOUT_UNTIL = "pin_lockout_until"
    }
}
