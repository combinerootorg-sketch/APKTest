package com.example.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.TripTimerApplication
import com.example.domain.model.BackupFrequency
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class TripBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? TripTimerApplication ?: return Result.failure()
        val connectionState = app.backupRepository.driveConnectionState.first()

        if (!connectionState.isConnected) {
            return Result.success()
        }

        val backupResult = app.backupRepository.performBackup()
        return if (backupResult.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}

object BackupScheduler {
    private const val PERIODIC_WORK_NAME = "trip_timer_periodic_backup_work"
    private const val ONE_TIME_WORK_NAME = "trip_timer_one_time_backup_work"

    fun scheduleBackup(context: Context, enabled: Boolean, frequency: BackupFrequency) {
        try {
            val workManager = WorkManager.getInstance(context)

            if (!enabled || frequency == BackupFrequency.AFTER_COMPLETED_TRIP) {
                workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val intervalDays = if (frequency == BackupFrequency.WEEKLY) 7L else 1L

            val periodicRequest = PeriodicWorkRequestBuilder<TripBackupWorker>(intervalDays, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
        } catch (e: Throwable) {
            // WorkManager may not be initialized in test or low-resource contexts
        }
    }

    fun triggerOneTimeBackup(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<TripBackupWorker>()
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        } catch (e: Throwable) {
            // Gracefully ignore in non-workmanager environments
        }
    }
}
