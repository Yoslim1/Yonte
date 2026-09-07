package com.yonte.core.backup

import android.content.ContentResolver
import android.net.Uri
import com.yonte.core.security.EncryptionManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
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
    private val backupCodec = BackupCodec()

    override fun exportNotes(resolver: ContentResolver, uri: Uri, notes: List<BackupNote>) {
        writeEnvelope(resolver, uri, buildNotesPayload(notes), encryptionManager::encrypt)
    }

    override fun importNotes(resolver: ContentResolver, uri: Uri): List<BackupNote> =
        readEnvelope(resolver, uri, encryptionManager::decrypt)

    override fun exportNotes(resolver: ContentResolver, uri: Uri, notes: List<BackupNote>, passphrase: CharArray) {
        writeEnvelope(resolver, uri, buildNotesPayload(notes)) { backupCodec.encrypt(it, passphrase) }
    }

    override fun exportNotes(resolver: ContentResolver, uri: Uri, notes: List<BackupNote>, key: ByteArray, salt: ByteArray) {
        writeEnvelope(resolver, uri, buildNotesPayload(notes)) { backupCodec.encryptWithKey(it, key, salt) }
    }

    override fun importNotes(resolver: ContentResolver, uri: Uri, passphrase: CharArray): List<BackupNote> =
        readEnvelope(resolver, uri) { backupCodec.decrypt(it, passphrase) }

    private fun writeEnvelope(
        resolver: ContentResolver,
        uri: Uri,
        payload: ByteArray,
        encrypt: (ByteArray) -> ByteArray,
    ) {
        val envelope = buildEncryptedEnvelope(payload, encrypt)
        resolver.openOutputStream(uri)?.use { it.write(envelope.toByteArray(Charsets.UTF_8)) }
            ?: error("Unable to open backup destination")
    }

    private fun readEnvelope(
        resolver: ContentResolver,
        uri: Uri,
        decrypt: (ByteArray) -> ByteArray,
    ): List<BackupNote> {
        val envelope = resolver.openInputStream(uri)?.use { readBackupBytes(it).toString(Charsets.UTF_8) }
            ?: error("Unable to open backup source")
        val envelopeJson = JSONObject(envelope)
        val encrypted = android.util.Base64.decode(envelopeJson.getString("payload"), android.util.Base64.DEFAULT)
        val payload = decrypt(encrypted)
        val root = JSONObject(payload.toString(Charsets.UTF_8))
        require(root.getString("format") == "ynote-backup") { "Unsupported backup format" }
        require(sha256(payload) == envelopeJson.getString("checksum")) { "Backup checksum mismatch" }
        val notes = root.getJSONArray("notes")
        return buildList(notes.length()) {
            for (index in 0 until notes.length()) {
                val item = notes.getJSONObject(index)
                add(parseBackupNote(item))
            }
        }
    }
}

/** Build the JSON payload containing all notes — shared by interactive export and
 * [ScheduledBackupWorker]. */
internal fun buildNotesPayload(notes: List<BackupNote>): ByteArray {
    val notesJson = JSONArray().apply {
        notes.forEach { note ->
            put(JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("body", note.body)
                put("is_pinned", note.isPinned)
                put("is_archived", note.isArchived)
                put("is_trashed", note.isTrashed)
                put("created_at", note.createdAt)
                put("updated_at", note.updatedAt)
            })
        }
    }
    return JSONObject().apply {
        put("format", "ynote-backup")
        put("schema_version", 1)
        put("created_at", System.currentTimeMillis())
        put("notes", notesJson)
    }.toString().toByteArray(Charsets.UTF_8)
}

/** Wrap a plaintext payload into the encrypted envelope format, ready for writing
 * to disk — shared by interactive export and [ScheduledBackupWorker]. */
internal fun buildEncryptedEnvelope(payload: ByteArray, encrypt: (ByteArray) -> ByteArray): String {
    val checksum = sha256(payload)
    val envelope = JSONObject().apply {
        put("format", "ynote-backup-encrypted")
        put("format_version", 1)
        put("checksum", checksum)
        put("payload", android.util.Base64.encodeToString(encrypt(payload), android.util.Base64.NO_WRAP))
    }
    return envelope.toString(2).also {
        require(it.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_FILE_BYTES) { "Backup exceeds the file size limit" }
    }
}

internal fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

data class BackupNote(
    val id: String,
    val title: String,
    val body: String,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
)

/** Import and export are capped at 16 MiB of encoded JSON, because decoding/decryption/JSON
 * parsing retain several copies in memory on mobile devices. Larger files fail
 * before decryption or any database writes; the export wire format is unchanged. */
internal const val MAX_BACKUP_FILE_BYTES = 16 * 1024 * 1024

internal fun readBackupBytes(input: InputStream, maxBytes: Int = MAX_BACKUP_FILE_BYTES): ByteArray {
    require(maxBytes >= 0) { "Invalid backup size limit" }
    val output = ByteArrayOutputStream(minOf(DEFAULT_BUFFER_SIZE, maxBytes))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (output.size() < maxBytes) {
        val count = input.read(buffer, 0, minOf(buffer.size, maxBytes - output.size()))
        if (count < 0) return output.toByteArray()
        if (count == 0) {
            // Do not spin indefinitely on a provider that returns an empty read.
            val next = input.read()
            if (next < 0) return output.toByteArray()
            output.write(next)
        } else {
            output.write(buffer, 0, count)
        }
    }
    require(input.read() == -1) { "Backup exceeds the import size limit" }
    return output.toByteArray()
}

internal fun parseBackupNote(item: JSONObject): BackupNote = BackupNote(
    item.getString("id"), item.getString("title"), item.getString("body"),
    item.optBoolean("is_pinned"), item.getLong("created_at"), item.getLong("updated_at"),
    item.optBoolean("is_archived", false), item.optBoolean("is_trashed", false),
)
