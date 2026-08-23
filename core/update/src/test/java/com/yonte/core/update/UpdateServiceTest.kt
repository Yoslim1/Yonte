package com.yonte.core.update

import org.junit.Assert.assertThrows
import org.junit.Test

class UpdateServiceTest {
    private val valid = UpdateInfo(
        versionCode = 3,
        versionName = "1.2.0",
        minimumSdk = 26,
        sha256 = "a".repeat(64),
        certificateSha256 = "b".repeat(64),
        downloadUrl = "https://github.com/Yoslim1/Yonte-updates/releases/download/v1.2.0/Yonte-v1.2.0.apk",
        releaseNotes = "test",
    )

    @Test
    fun acceptsTrustedHttpsReleaseUrl() {
        validateUpdateInfo(valid)
    }

    @Test
    fun rejectsHttpUrl() {
        assertThrows(IllegalArgumentException::class.java) { validateUpdateInfo(valid.copy(downloadUrl = valid.downloadUrl.replace("https", "http"))) }
    }

    @Test
    fun rejectsUntrustedHost() {
        assertThrows(IllegalArgumentException::class.java) { validateUpdateInfo(valid.copy(downloadUrl = valid.downloadUrl.replace("github.com", "example.com"))) }
    }

    @Test
    fun rejectsUntrustedPath() {
        assertThrows(IllegalArgumentException::class.java) { validateUpdateInfo(valid.copy(downloadUrl = "https://github.com/Yoslim1/other/releases/download/v1.2.0/app.apk")) }
    }

    @Test
    fun rejectsMalformedChecksum() {
        assertThrows(IllegalArgumentException::class.java) { validateUpdateInfo(valid.copy(sha256 = "not-a-checksum")) }
    }

    @Test
    fun rejectsNonPositiveVersion() {
        assertThrows(IllegalArgumentException::class.java) { validateUpdateInfo(valid.copy(versionCode = 0)) }
    }
}
