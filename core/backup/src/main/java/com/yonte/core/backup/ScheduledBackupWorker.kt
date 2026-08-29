package com.yonte.core.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yonte.core.database.NoteRepository
import com.yonte.core.database.YonteDatabase
import com.yonte.core.security.EncryptionManager
import com.yonte.core.security.LocalKeyManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScheduledBackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    /** Test seam: lets tests supply a fake key manager without touching
     * AndroidKeyStore. Defaults to the production session-key provider. */
    internal var keyManagerProvider: (Context) -> LocalKeyManager =
        { LocalKeyManager(it, EncryptionManager()) }

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val destinationUri = prefs.getString(KEY_DESTINATION_URI, null)
            ?: return Result.success() // not configured, nothing to do
        val localKeyManager = keyManagerProvider(applicationContext)
        val key = localKeyManager.cachedAutoBackupKey() ?: return Result.success() // auto-backup key not cached this boot, skip quietly
        val salt = localKeyManager.currentSalt() ?: return Result.success()

        return try {
            val database = YonteDatabase.get(applicationContext, key)
            val repository = NoteRepository(database)
            val notes = repository.getAll().map { entity ->
                BackupNote(entity.id, entity.title, entity.body, entity.isPinned, entity.createdAt, entity.updatedAt)
            }
            val backupCodec = BackupCodec()
            val envelope = buildEncryptedEnvelope(buildNotesPayload(notes)) { backupCodec.encryptWithKey(it, key, salt) }
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))
            val fileName = "yonte-backup-$timestamp.ybk"
            val tree = DocumentFile.fromTreeUri(applicationContext, Uri.parse(destinationUri))
                ?: return Result.failure()
            val file = tree.createFile("application/octet-stream", fileName)
                ?: return Result.failure()
            applicationContext.contentResolver.openOutputStream(file.uri)?.use { stream ->
                stream.write(envelope.toByteArray(Charsets.UTF_8))
            } ?: return Result.failure()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val PREFS_NAME = "yonte_auto_backup"
        const val KEY_DESTINATION_URI = "destination_uri"
    }
}
