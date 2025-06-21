package com.ramitsuri.notificationjournal.work

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelType

class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ServiceLocator.repository.uploadAll()
        ServiceLocator.onAppStop()
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification =
            NotificationCompat.Builder(
                applicationContext,
                NotificationChannelType.MAIN.id,
            ).apply {
                setSmallIcon(R.drawable.ic_notification)
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                setContentTitle(
                    applicationContext.getString(R.string.upload_worker_notification_title),
                )
            }.build()
        return ForegroundInfo(2, notification)
    }

    companion object {
        private const val WORK_TAG = "TAG-UploadWorker"

        fun enqueueImmediate(context: Context) {
            return OneTimeWorkRequest
                .Builder(UploadWorker::class)
                .addTag(WORK_TAG)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
                .let { request ->
                    WorkManager
                        .getInstance(context)
                        .enqueue(request)
                }
        }
    }
}
