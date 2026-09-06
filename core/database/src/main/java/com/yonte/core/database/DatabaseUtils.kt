package com.yonte.core.database

/** Room reports a missing migration path (downgrade to an older APK, or a forgotten
 * upgrade migration) as an [IllegalStateException] whose message states that
 * "A migration from X to Y was required but not found". The failure may be wrapped,
 * so the whole cause chain is inspected. A match means the encrypted data is intact
 * but this app version must not proceed: the caller must close the database and
 * surface the blocking UI instead of unlocking into crashes. */
fun isDatabaseVersionMismatch(error: Throwable): Boolean {
    var current: Throwable? = error
    while (current != null) {
        if (current is IllegalStateException) {
            val message = current.message
            if (message != null &&
                message.contains("A migration from") &&
                message.contains("was required but not found")
            ) {
                return true
            }
        }
        current = current.cause
    }
    return false
}
