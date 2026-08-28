package com.yonte.feature.settings

import android.content.ContentResolver
import android.net.Uri
import com.yonte.core.backup.BackupGateway
import com.yonte.core.backup.BackupNote

/**
 * Single seam through which the Settings UI invokes backups. Every call must land on
 * the passphrase-protected [BackupGateway] overloads so exports stay portable across
 * devices; [BackupGatewayWiringTest] fails if the legacy device-bound path returns.
 */
internal object PassphraseBackupFlow {
    fun export(gateway: BackupGateway, resolver: ContentResolver, uri: Uri, notes: List<BackupNote>, passphrase: CharArray) {
        gateway.exportNotes(resolver, uri, notes, passphrase)
    }

    fun exportWithKey(gateway: BackupGateway, resolver: ContentResolver, uri: Uri, notes: List<BackupNote>, key: ByteArray, salt: ByteArray) {
        gateway.exportNotes(resolver, uri, notes, key, salt)
    }

    fun import(gateway: BackupGateway, resolver: ContentResolver, uri: Uri, passphrase: CharArray): List<BackupNote> =
        gateway.importNotes(resolver, uri, passphrase)
}
