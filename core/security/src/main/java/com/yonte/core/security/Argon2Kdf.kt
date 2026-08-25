package com.yonte.core.security

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.security.SecureRandom

object Argon2Kdf {
    private const val SALT_SIZE = 16
    private const val KEY_SIZE = 32 // 256-bit key for SQLCipher / AES-GCM
    // Tuned for a low/mid-range device (target: ~1-2s on OPPO A9 2020 class hardware).
    // Verify actual timing on-device before shipping; adjust memoryCostKib downward
    // first if unlock feels slow, iterations second.
    private const val ITERATIONS = 3
    private const val MEMORY_COST_KIB = 65536 // 64 MB
    private const val PARALLELISM = 2

    data class DerivedKey(val key: ByteArray, val salt: ByteArray)

    // argon2kt ships Android-only native libraries, so plain-JVM unit tests cannot run
    // the real KDF. Production always uses the Argon2id engine below; only tests swap
    // this seam (restored via installKdfEngineForTesting(null)), keeping
    // determinism/salt-sensitivity properties covered on CI while real-Argon2 behavior
    // is exercised by instrumented tests.
    internal var kdfEngine: (password: ByteArray, salt: ByteArray) -> ByteArray = ::argon2idEngine
    private val defaultKdfEngine: (password: ByteArray, salt: ByteArray) -> ByteArray = ::argon2idEngine

    /** Test-only hook for JVM unit tests across modules. Pass null to restore the
     * real Argon2id engine. Never call from production code paths. */
    fun installKdfEngineForTesting(engine: ((password: ByteArray, salt: ByteArray) -> ByteArray)?) {
        kdfEngine = engine ?: defaultKdfEngine
    }

    fun deriveNewKey(passphrase: CharArray): DerivedKey {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        return DerivedKey(derive(passphrase, salt), salt)
    }

    fun deriveWithSalt(passphrase: CharArray, salt: ByteArray): ByteArray =
        derive(passphrase, salt)

    private fun derive(passphrase: CharArray, salt: ByteArray): ByteArray =
        kdfEngine(String(passphrase).toByteArray(Charsets.UTF_8), salt)

    private fun argon2idEngine(password: ByteArray, salt: ByteArray): ByteArray {
        val argon2Kt = Argon2Kt()
        val result = argon2Kt.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = password,
            salt = salt,
            tCostInIterations = ITERATIONS,
            mCostInKibibyte = MEMORY_COST_KIB,
            parallelism = PARALLELISM,
            hashLengthInBytes = KEY_SIZE,
        )
        return result.rawHashAsByteArray()
    }
}
