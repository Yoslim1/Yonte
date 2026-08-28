package com.yonte.core.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.yonte.core.security.LocalKeyManager
import com.yonte.core.security.SessionKeyCipher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScheduledBackupWorkerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(ScheduledBackupWorker.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun `worker returns success when no destination is configured`() {
        val worker = TestListenableWorkerBuilder<ScheduledBackupWorker>(context).build()
        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `worker returns success when destination is set but session key is unavailable`() {
        context.getSharedPreferences(ScheduledBackupWorker.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(ScheduledBackupWorker.KEY_DESTINATION_URI, "content://fake/tree").commit()
        val worker = TestListenableWorkerBuilder<ScheduledBackupWorker>(context).build()
        worker.keyManagerProvider = { ctx -> LocalKeyManager(ctx, FakeSessionKeyCipher()) }
        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
    }
}

private class FakeSessionKeyCipher : SessionKeyCipher {
    override fun encrypt(plain: ByteArray): ByteArray = plain
    override fun decrypt(payload: ByteArray): ByteArray = payload
}
