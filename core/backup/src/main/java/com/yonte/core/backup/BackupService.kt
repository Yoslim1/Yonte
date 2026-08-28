package com.yonte.core.backup

import android.content.ContentResolver
import android.net.Uri
import com.yonte.core.security.EncryptionManager
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

interface BackupGateway {
    fun exportNotes(resolver: ContentResolver, uri: Uri, notes: List<BackupNote>)
    fun importNotes(resolver: ContentResolver, uri: Uri): List<BackupNote>

    /** Passphrase-protected variants backed by BackupCodec. The backup passphrase is
     * intentionally independent from the local unlock passphrase. The legacy
     * no-passphrase methods remain until the settings UI grows a backup-passphrase
     * prompt (out of scope for the current task). */
    fun exportNotes(resolver: ContentResolver, uri: Uri, notes: List<BackupNote>, passphrase: CharArray)
    fun importNotes(resolver: ContentResolver, uri: Uri, passphrase: CharArray): List<BackupNote>

    /** Key-based export that reuses the already-derived session key and local salt,
     * avoiding a redundant Argon2 re-derivation at every export. */
    fun exportNotes(resolver: ContentResolver, uri: Uri, notes: List<BackupNote>, key: ByteArray, salt: ByteArray)
}

class BackupService(private val encryptionManager: EncryptionManager) : BackupGateway {
    companion object {
        private const val FORMAT_VERSION = 1
    }

    private val backupCodec = BackupCodec()

    override fun exportNotes(resolver: ContentResolver, uri: Uri, notes: List<BackupNote>) {
        writeEnvelope(resolver, uri, notesPayload(notes), encryptionManager::encrypt)
    }

    override fun importNotes(resolver: ContentResolver, uri: Uri): List<BackupNote> =
        readEnvelope(resolver, uri, encryptionManager::decrypt)

    override fun exportNotes(resolver: ContentResolver, uri: Uri, notes: List<BackupNote>, passphrase: CharArray) {
        writeEnvelope(resolver, uri, notesPayload(notes)) { backupCodec.encrypt(it, passphrase) }
    }

    override fun exportNotes(resolver: ContentResolver, uri: Uri, notes: List<BackupNote>, key: ByteArray, salt: ByteArray) {
        writeEnvelope(resolver, uri, notesPayload(notes)) { backupCodec.encryptWithKey(it, key, salt) }
    }

    override fun importNotes(resolver: ContentResolver, uri: Uri, passphrase: CharArray): List<BackupNote> =
        readEnvelope(resolver, uri) { backupCodec.decrypt(it, passphrase) }

    private fun notesPayload(notes: List<BackupNote>): ByteArray {
        val notesJson = JSONArray().apply {
            notes.forEach { note ->
                put(JSONObject().apply {
                    put("id", note.id)
                    put("title", note.title)
                    put("body", note.body)
                    put("is_pinned", note.isPinned)
                    put("created_at", note.createdAt)
                    put("updated_at", note.updatedAt)
                })
            }
        }
        return JSONObject().apply {
            put("format", "ynote-backup")
            put("schema_version", FORMAT_VERSION)
            put("created_at", System.currentTimeMillis())
            put("notes", notesJson)
        }.toString().toByteArray(Charsets.UTF_8)
    }

    private fun writeEnvelope(
        resolver: ContentResolver,
        uri: Uri,
        payload: ByteArray,
        encrypt: (ByteArray) -> ByteArray,
    ) {
        val checksum = sha256(payload)
        val envelope = JSONObject().apply {
            put("format", "ynote-backup-encrypted")
            put("format_version", FORMAT_VERSION)
            put("checksum", checksum)
            put("payload", android.util.Base64.encodeToString(encrypt(payload), android.util.Base64.NO_WRAP))
        }
        resolver.openOutputStream(uri)?.use { it.write(envelope.toString(2).toByteArray(Charsets.UTF_8)) }
            ?: error("Unable to open backup destination")
    }

    private fun readEnvelope(
        resolver: ContentResolver,
        uri: Uri,
        decrypt: (ByteArray) -> ByteArray,
    ): List<BackupNote> {
        val envelope = resolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("Unable to open backup source")
        val encrypted = android.util.Base64.decode(JSONObject(envelope).getString("payload"), android.util.Base64.DEFAULT)
        val payload = decrypt(encrypted)
        val root = JSONObject(payload.toString(Charsets.UTF_8))
        require(root.getString("format") == "ynote-backup") { "Unsupported backup format" }
        require(sha256(payload) == JSONObject(envelope).getString("checksum")) { "Backup checksum mismatch" }
        val notes = root.getJSONArray("notes")
        return buildList(notes.length()) {
            for (index in 0 until notes.length()) {
                val item = notes.getJSONObject(index)
                add(BackupNote(item.getString("id"), item.getString("title"), item.getString("body"), item.optBoolean("is_pinned"), item.getLong("created_at"), item.getLong("updated_at")))
            }
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

data class BackupNote(
    val id: String,
    val title: String,
    val body: String,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
