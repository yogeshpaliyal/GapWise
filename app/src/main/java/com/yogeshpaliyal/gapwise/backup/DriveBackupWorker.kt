package com.yogeshpaliyal.gapwise.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yogeshpaliyal.gapwise.app
import java.util.concurrent.TimeUnit

/**
 * Daily backup to Google Drive, when the user has turned it on and already
 * granted Drive access. It never shows a consent screen: if access was
 * revoked, it skips quietly and the next manual backup asks again.
 */
class DriveBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!ctx.app.prefs.driveAutoBackup) return Result.success()
        return try {
            when (val auth = DriveBackup.authorize(ctx)) {
                is DriveBackup.Auth.NeedsConsent -> Result.success()
                is DriveBackup.Auth.Granted -> {
                    val content = ctx.app.repository.exportJson()
                    val remote = DriveBackup.upload(auth.token, content)
                    ctx.app.prefs.lastDriveBackupAt = remote.modifiedAt
                    Result.success()
                }
            }
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK = "gapwise-drive-backup"

        fun schedule(context: Context) {
            val wm = WorkManager.getInstance(context)
            if (!context.app.prefs.driveAutoBackup) {
                wm.cancelUniqueWork(WORK)
                return
            }
            val request = PeriodicWorkRequestBuilder<DriveBackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .setRequiresBatteryNotLow(true)
                        .build(),
                )
                .build()
            wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
