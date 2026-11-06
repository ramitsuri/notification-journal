package com.ramitsuri.notificationjournal.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import dev.zacsweers.metro.Inject

class AppWorkerFactory
    @Inject
    constructor(
        private val voiceNoteProcessWorkerFactory: VoiceNoteProcessWorker.Factory,
        private val showNotificationWorkerFactory: ShowNotificationWorker.Factory,
        private val uploadWorkerFactory: UploadWorker.Factory,
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ): ListenableWorker? =
            when (workerClassName) {
                VoiceNoteProcessWorker::class.java.name ->
                    voiceNoteProcessWorkerFactory.create(appContext, workerParameters)
                ShowNotificationWorker::class.java.name ->
                    showNotificationWorkerFactory.create(appContext, workerParameters)
                UploadWorker::class.java.name ->
                    uploadWorkerFactory.create(appContext, workerParameters)
                else -> null
            }
    }
