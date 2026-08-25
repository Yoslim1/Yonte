package com.yonte.core.security

/**
 * Symmetric in-memory wrapping of the passphrase-derived local key so the raw key
 * material never sits on disk unencrypted. Implemented by EncryptionManager with an
 * AndroidKeyStore-backed AES-GCM key; kept as an interface so LocalKeyManager stays
 * unit-testable on the JVM.
 */
interface SessionKeyCipher {
    fun encrypt(plain: ByteArray): ByteArray
    fun decrypt(payload: ByteArray): ByteArray
}
