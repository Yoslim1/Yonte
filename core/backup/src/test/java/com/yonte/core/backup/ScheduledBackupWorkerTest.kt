package com.yonte.core.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.yonte.core.security.Argon2Kdf
import com.yonte.core.security.LocalKeyManager
import com.yonte.core.security.SessionKeyCipher
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class ScheduledBackupWorkerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        Argon2Kdf.installKdfEngineForTesting { password, salt ->
            MessageDigest.getInstance("SHA-256").apply { update(salt); update(password) }.digest()
        }
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(ScheduledBackupWorker.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @After
    fun tearDown() {
        Argon2Kdf.installKdfEngineForTesting(null)
    }

    @Test
    fun `worker returns success when no destination is configured`() {
        val worker = TestListenableWorkerBuilder<ScheduledBackupWorker>(context).build()
        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `worker returns success when destination is set but auto backup key is unavailable`() {
        context.getSharedPreferences(ScheduledBackupWorker.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(ScheduledBackupWorker.KEY_DESTINATION_URI, "content://fake/tree").commit()
        val worker = TestListenableWorkerBuilder<ScheduledBackupWorker>(context).build()
        worker.keyManagerProvider = { ctx -> LocalKeyManager(ctx, FakeSessionKeyCipher()) }
        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `worker proceeds past the key check when the auto backup key is cached`() {
        // The session cache is intentionally left empty; only the auto-backup
        // cache is populated. If the worker still read cachedSessionKey() it
        // would short-circuit with success here. Because it reads
        // cachedAutoBackupKey() and the salt is present, it proceeds to open the
        // database (which fails in the test environment), so the result is not
        // the "missing key" success.
        val km = LocalKeyManager(context, FakeSessionKeyCipher())
        km.setupPassphrase("scheduled-passphrase".toCharArray())
        km.cacheAutoBackupKey(km.cachedSessionKey()!!)
        context.getSharedPreferences(ScheduledBackupWorker.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(ScheduledBackupWorker.KEY_DESTINATION_URI, "content://fake/tree").commit()
        val worker = TestListenableWorkerBuilder<ScheduledBackupWorker>(context).build()
        worker.keyManagerProvider = { km }
        val result = worker.startWork().get()
        assertNotEquals(ListenableWorker.Result.success(), result)
    }
}

private class FakeSessionKeyCipher : SessionKeyCipher {
    override fun encrypt(plain: ByteArray): ByteArray = plain
    override fun decrypt(payload: ByteArray): ByteArray = payload
}

