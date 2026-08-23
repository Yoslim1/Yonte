package com.yonte.core.backup

import android.content.ContentResolver
import android.net.Uri
import com.yonte.core.security.EncryptionManager
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

class BackupService(private val encryptionManager: EncryptionManager) {
    companion object {
        private const val FORMAT_VERSION = 1
    }

    fun exportNotes(resolver: ContentResolver, uri: Uri, notes: List<BackupNote>) {
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
        val payload = JSONObject().apply {
            put("format", "ynote-backup")
            put("schema_version", FORMAT_VERSION)
            put("created_at", System.currentTimeMillis())
            put("notes", notesJson)
        }.toString().toByteArray(Charsets.UTF_8)
        val checksum = sha256(payload)
        val envelope = JSONObject().apply {
            put("format", "ynote-backup-encrypted")
            put("format_version", FORMAT_VERSION)
            put("checksum", checksum)
            put("payload", android.util.Base64.encodeToString(encryptionManager.encrypt(payload), android.util.Base64.NO_WRAP))
        }
        resolver.openOutputStream(uri)?.use { it.write(envelope.toString(2).toByteArray(Charsets.UTF_8)) }
            ?: error("Unable to open backup destination")
    }

    fun importNotes(resolver: ContentResolver, uri: Uri): List<BackupNote> {
        val envelope = resolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("Unable to open backup source")
        val encrypted = android.util.Base64.decode(JSONObject(envelope).getString("payload"), android.util.Base64.DEFAULT)
        val payload = encryptionManager.decrypt(encrypted)
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
