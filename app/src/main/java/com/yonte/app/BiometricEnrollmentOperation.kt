package com.yonte.app

/**
 * Owns the session-key buffer for one asynchronous biometric enrollment.
 *
 * The first terminal callback claims the operation before persistence or cleanup
 * begins. This prevents a duplicate biometric callback from reusing the key,
 * persisting it again, or clearing it more than once.
 */
internal class BiometricEnrollmentOperation(
    private val sessionKey: ByteArray,
    private val onResult: (Boolean) -> Unit,
) {
    private val terminalLock = Any()
    private var terminal = false

    fun succeed(persist: (ByteArray) -> Unit) {
        if (!claimTerminal()) return

        val success = try {
            persist(sessionKey)
            true
        } catch (_: Exception) {
            false
        } finally {
            sessionKey.fill(0)
        }

        onResult(success)
    }

    fun fail() {
        if (!claimTerminal()) return

        sessionKey.fill(0)
        onResult(false)
    }

    private fun claimTerminal(): Boolean = synchronized(terminalLock) {
        if (terminal) {
            false
        } else {
            terminal = true
            true
        }
    }
}
