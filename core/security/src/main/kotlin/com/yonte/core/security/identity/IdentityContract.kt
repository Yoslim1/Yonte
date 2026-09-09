package com.yonte.core.security.identity

/**
 * Stable identity contract for security-aware entities.
 *
 * Features must not depend on storage implementation details.
 * Identity is the reference used for ownership, permissions and auditing.
 */
interface IdentityContract {
    val id: String
    val ownerId: String
}
