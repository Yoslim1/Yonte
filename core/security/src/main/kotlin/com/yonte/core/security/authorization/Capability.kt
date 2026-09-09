package com.yonte.core.security.authorization

/**
 * Stable permission vocabulary shared across features.
 *
 * Features own their data and workflows; this contract only defines
 * authorization language and does not grant access by itself.
 */
sealed interface Capability {
    val value: String

    data object ReadNotes : Capability {
        override val value: String = "notes.read"
    }

    data object CreateNotes : Capability {
        override val value: String = "notes.create"
    }

    data object UpdateNotes : Capability {
        override val value: String = "notes.update"
    }

    data object DeleteNotes : Capability {
        override val value: String = "notes.delete"
    }
}
