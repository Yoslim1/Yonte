package com.yonte.core.backup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration

/** Maps user-facing backup frequency to the repeat interval for WorkManager. */
internal fun frequencyToDuration(frequency: BackupFrequency): Duration = when (frequency) {
    BackupFrequency.WEEKLY -> Duration.ofDays(7)
    BackupFrequency.BIWEEKLY -> Duration.ofDays(14)
    BackupFrequency.MONTHLY -> Duration.ofDays(30)
    BackupFrequency.OFF -> Duration.ZERO
}

enum class BackupFrequency { WEEKLY, BIWEEKLY, MONTHLY, OFF }

object AutoBackupScheduler {

    fun schedule(context: Context, frequency: BackupFrequency) {
        val duration = frequencyToDuration(frequency)
        if (duration.isZero) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<ScheduledBackupWorker>(duration)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private const val WORK_NAME = "yonte_auto_backup"
}
