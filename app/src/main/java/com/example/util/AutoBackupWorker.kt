package com.example.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase
import java.util.concurrent.TimeUnit

/**
 * Runs a scheduled local backup in the background via WorkManager, so "Daily" / "Weekly"
 * auto-backup in Settings actually produces a new backup file on that schedule instead of
 * only running when the user manually taps "Create Backup".
 *
 * Auto-backups are NOT password-encrypted: encrypting them would require persisting the
 * user's backup password into WorkManager's own on-disk work database, which would store
 * that password in plaintext outside the app's control -- exactly what backup encryption
 * is meant to prevent. If encrypted auto-backups are needed later, that requires deriving
 * the encryption key from Android Keystore (hardware-backed) rather than a remembered
 * password, which is a separate piece of work.
 */
class AutoBackupWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val backupManager = BackupManager(applicationContext, db)
            val retentionCount = inputData.getInt(KEY_RETENTION_COUNT, 5)

            val result = backupManager.createBackup(
                password = "",
                isSafetyBackup = false,
                retentionCount = retentionCount
            )

            if (result.isSuccess) {
                // createBackup() already prunes older backups down to retentionCount
                // internally once the new backup is written and verified -- see
                // BackupManager.pruneBackups(), which only removes older files after the
                // new backup has been created, matching the "only prune after verifying
                // the newer backup succeeded" requirement.
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "rg_pos_auto_backup"
        private const val KEY_RETENTION_COUNT = "retention_count"

        /**
         * (Re)schedules the periodic auto-backup job. Call this whenever the user changes
         * the auto-backup setting in Settings/Backup & Restore, and also once on app
         * startup so the schedule survives a process restart even if WorkManager's own
         * persisted job state was somehow cleared.
         *
         * @param frequency one of "OFF", "DAILY", "WEEKLY"
         */
        fun schedule(context: Context, frequency: String, retentionCount: Int) {
            val workManager = WorkManager.getInstance(context)

            if (frequency == "OFF") {
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
                return
            }

            val intervalDays = if (frequency == "WEEKLY") 7L else 1L

            val inputData = Data.Builder()
                .putInt(KEY_RETENTION_COUNT, retentionCount)
                .build()

            val constraints = Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(intervalDays, TimeUnit.DAYS)
                .setInputData(inputData)
                .setConstraints(constraints)
                .build()

            // UPDATE (not KEEP): if the user changes Daily -> Weekly or the retention
            // count, the new schedule/input should take effect rather than the old one
            // silently continuing to run.
            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
