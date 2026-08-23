package com.yonte.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

private const val UPDATE_MANIFEST_URL = "https://raw.githubusercontent.com/Yoslim1/Yonte-updates/main/update.json"

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val minimumSdk: Int,
    val sha256: String,
    val downloadUrl: String,
    val releaseNotes: String,
)

class UpdateService(private val context: Context) {
    suspend fun checkForUpdate(currentVersionCode: Int): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
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
                    downloadUrl = json.getString("downloadUrl"),
                    releaseNotes = json.optString("releaseNotes"),
                )
                if (info.versionCode > currentVersionCode && android.os.Build.VERSION.SDK_INT >= info.minimumSdk) info else null
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun downloadAndVerify(info: UpdateInfo): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apk = File(dir, "Yonte-${info.versionName}.apk")
            URL(info.downloadUrl).openStream().use { input -> apk.outputStream().use { output -> input.copyTo(output) } }
            require(sha256(apk) == info.sha256) { "Downloaded update checksum mismatch" }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        }
    }

    fun install(uri: Uri) {
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }

    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
}
