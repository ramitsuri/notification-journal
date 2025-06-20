package com.ramitsuri.notificationjournal.work

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker.Result.failure
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.ui.nav.DeepLink
import com.ramitsuri.notificationjournal.core.ui.nav.uriWithArgsValues
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelType
import com.ramitsuri.notificationjournal.core.utils.NotificationInfo
import com.ramitsuri.notificationjournal.core.utils.dayMonthDateWithYearSuspend
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class ShowNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val assignmentId = inputData.getString(JOURNAL_ENTRY_ID_KEY)
        if (assignmentId == null) {
            Log.v(TAG, "journal entry id null. Cannot show notification")
            return failure()
        }
        val journalEntry =
            ServiceLocator.repository.get(assignmentId) ?: run {
                Log.v(TAG, "journal entry not found. Cannot show notification")
                return failure()
            }
        showNotification(journalEntry)
        return Result.success()
    }

    private suspend fun showNotification(journalEntry: JournalEntry) {
        NotificationInfo(
            channel = NotificationChannelType.REMINDERS,
            title = dayMonthDateWithYearSuspend(journalEntry.entryTime.date),
            body = journalEntry.text,
            iconResId = R.drawable.ic_notification,
            isVisibilityPublic = false,
            cancelOnTouch = true,
            isForegroundServiceImmediate = false,
            isOngoing = false,
            clickDeepLinkUri = DeepLink.REMINDER.uriWithArgsValues(listOf(journalEntry.id)),
        ).let { notificationInfo ->
            ServiceLocator.notificationHandler.showNotification(notificationInfo)
        }
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
        private const val TAG = "ShowNotificationWorker"
        private const val WORK_TAG = "TAG-ShowNotificationWorker"
        private const val JOURNAL_ENTRY_ID_KEY = "journal_entry_id_key"

        fun enqueue(
            context: Context,
            journalEntryId: String,
            showIn: Duration,
        ) {
            val workName = WORK_TAG.plus(journalEntryId)
            val inputData =
                workDataOf(
                    JOURNAL_ENTRY_ID_KEY to journalEntryId,
                )
            return OneTimeWorkRequest
                .Builder(ShowNotificationWorker::class)
                .addTag(WORK_TAG)
                .addTag(journalEntryId)
                .setInitialDelay(showIn.toJavaDuration())
                .setInputData(inputData)
                .build()
                .let { request ->
                    WorkManager
                        .getInstance(context)
                        .enqueueUniqueWork(
                            uniqueWorkName = workName,
                            existingWorkPolicy = ExistingWorkPolicy.REPLACE,
                            request = request,
                        )
                }
        }
    }
}
