package com.yonte.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

private const val UPDATE_MANIFEST_URL = "https://raw.githubusercontent.com/Yoslim1/Yonte-updates/main/update.json"
private const val TRUSTED_HOST = "github.com"
private const val TRUSTED_PATH_PREFIX = "/Yoslim1/Yonte-updates/releases/download/"
private const val MAX_APK_BYTES = 200L * 1024L * 1024L

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val minimumSdk: Int,
    val sha256: String,
    val certificateSha256: String,
    val downloadUrl: String,
    val releaseNotes: String,
)

internal fun validateUpdateInfo(info: UpdateInfo) {
    require(info.versionCode > 0) { "Invalid update versionCode" }
    require(info.versionName.isNotBlank()) { "Invalid update versionName" }
    require(info.minimumSdk in 1..Build.VERSION_CODES.CUR_DEVELOPMENT) { "Invalid minimum SDK" }
    require(info.sha256.matches(Regex("[0-9a-fA-F]{64}"))) { "Invalid update checksum" }
    require(info.certificateSha256.matches(Regex("[0-9a-fA-F]{64}"))) { "Invalid signing certificate checksum" }
    val uri = URI(info.downloadUrl)
    require(uri.scheme.equals("https", ignoreCase = true)) { "Update URL must use HTTPS" }
    require(uri.host.equals(TRUSTED_HOST, ignoreCase = true)) { "Untrusted update host" }
    require(uri.path.startsWith(TRUSTED_PATH_PREFIX)) { "Untrusted update path" }
}

interface UpdateGateway {
    suspend fun checkForUpdate(currentVersionCode: Int): Result<UpdateInfo?>
    suspend fun downloadAndVerify(info: UpdateInfo): Result<Uri>
    fun install(uri: Uri)
}

class UpdateService(context: Context) : UpdateGateway {
    private val context = context.applicationContext

    override suspend fun checkForUpdate(currentVersionCode: Int): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(UPDATE_MANIFEST_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache")
            }
            try {
                require(connection.responseCode in 200..299) { "Update manifest HTTP ${connection.responseCode}" }
                val json = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
                val info = UpdateInfo(
                    versionCode = json.getInt("versionCode"),
                    versionName = json.getString("versionName"),
                    minimumSdk = json.getInt("minimumSdk"),
                    sha256 = json.getString("sha256").lowercase(),
                    certificateSha256 = json.getString("certificateSha256").lowercase(),
                    downloadUrl = json.getString("downloadUrl"),
                    releaseNotes = json.optString("releaseNotes"),
                )
                validateUpdateInfo(info)
                if (info.versionCode > currentVersionCode && Build.VERSION.SDK_INT >= info.minimumSdk) info else null
            } finally {
                connection.disconnect()
            }
        }
    }

    override suspend fun downloadAndVerify(info: UpdateInfo): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            validateUpdateInfo(info)
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apk = File(dir, "Yonte-${info.versionCode}-${info.versionName}.apk")
            val connection = (URL(info.downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 30_000
                requestMethod = "GET"
            }
            try {
                require(connection.responseCode in 200..299) { "Update download HTTP ${connection.responseCode}" }
                require(connection.contentLengthLong <= MAX_APK_BYTES) { "Update is too large" }
                connection.inputStream.use { input -> apk.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= MAX_APK_BYTES) { "Update is too large" }
                        output.write(buffer, 0, read)
                    }
                } }
            } finally {
                connection.disconnect()
            }
            require(sha256(apk) == info.sha256) { "Downloaded update checksum mismatch" }
            require(certificateSha256(apk) == info.certificateSha256) { "Downloaded update signing certificate mismatch" }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        }
    }

    override fun install(uri: Uri) {
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }

    private fun certificateSha256(file: File): String {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val packageInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION") packageInfo?.signatures
        }
        require(!signatures.isNullOrEmpty()) { "APK has no signing certificate" }
        return MessageDigest.getInstance("SHA-256").digest(signatures.first().toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").let { digest ->
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}
