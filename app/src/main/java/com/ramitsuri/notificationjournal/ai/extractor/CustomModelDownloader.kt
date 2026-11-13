package com.ramitsuri.notificationjournal.ai.extractor

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import co.touchlab.kermit.Logger
import com.google.ai.edge.litertlm.Backend
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

private const val TAG = "CustomModelDownloader"

sealed interface CustomModelDownloadState {
    data object Idle : CustomModelDownloadState

    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercent: Int,
    ) : CustomModelDownloadState

    data object Completed : CustomModelDownloadState

    data class Failed(
        val error: String,
    ) : CustomModelDownloadState
}

class CustomModelDownloader
    @Inject
    constructor(
        private val context: Application,
    ) {
        private val downloadManager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        private var activeDownloadId: Long? = null

        fun getModelFile(customPath: String? = null): File? = CustomModelHelper.getModelFile(context, customPath)

        fun createExtractor(
            prefManager: PrefManager? = null,
            backend: Backend = Backend.CPU(),
        ): AddEntryIntentExtractor =
            IntentExtractorFactory.createAddEntryExtractor(
                context = context,
                prefManager = prefManager,
                backend = backend,
            )

        fun startDownload(model: SupportedCustomModel = SupportedCustomModel.QWEN_2_5_1_5B): Long {
            cancelDownload()

            val destinationDir = context.getExternalFilesDir(null) ?: context.filesDir
            val targetFile = File(destinationDir, model.fileName)
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val request =
                DownloadManager
                    .Request(Uri.parse(model.downloadUrl))
                    .setTitle("Downloading AI Model")
                    .setDescription("${model.displayName} (.litertlm)")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(targetFile))
                    .setAllowedOverMetered(true)

            val id = downloadManager.enqueue(request)
            activeDownloadId = id
            Logger.withTag(TAG).i {
                "Enqueued model download ID: $id for ${model.fileName} to ${targetFile.absolutePath}"
            }
            return id
        }

        fun pollDownloadProgress(downloadId: Long): Flow<CustomModelDownloadState> =
            flow {
                var isFinished = false
                while (!isFinished) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    var cursor: Cursor? = null
                    try {
                        cursor = downloadManager.query(query)
                        if (cursor != null && cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val status = cursor.getInt(statusIndex)
                            val downloadedIndex =
                                cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val totalIndex =
                                cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                            val downloaded =
                                if (downloadedIndex != -1) {
                                    cursor.getLong(downloadedIndex)
                                } else {
                                    0L
                                }
                            val total =
                                if (totalIndex != -1) {
                                    cursor.getLong(totalIndex)
                                } else {
                                    0L
                                }
                            val progress =
                                if (total > 0) {
                                    ((downloaded * 100) / total).toInt()
                                } else {
                                    0
                                }

                            when (status) {
                                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED -> {
                                    emit(CustomModelDownloadState.Downloading(downloaded, total, progress))
                                }
                                DownloadManager.STATUS_PENDING -> {
                                    emit(CustomModelDownloadState.Downloading(0L, total, 0))
                                }
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    emit(CustomModelDownloadState.Completed)
                                    isFinished = true
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                                    val reason =
                                        if (reasonIndex != -1) {
                                            cursor.getInt(reasonIndex)
                                        } else {
                                            -1
                                        }
                                    emit(CustomModelDownloadState.Failed("Download failed with code $reason"))
                                    isFinished = true
                                }
                            }
                        } else {
                            emit(CustomModelDownloadState.Failed("Download not found"))
                            isFinished = true
                        }
                    } catch (e: Exception) {
                        emit(CustomModelDownloadState.Failed(e.message ?: "Unknown error"))
                        isFinished = true
                    } finally {
                        cursor?.close()
                    }
                    if (!isFinished) {
                        delay(500)
                    }
                }
            }.flowOn(Dispatchers.IO)

        fun cancelDownload() {
            activeDownloadId?.let { id ->
                try {
                    downloadManager.remove(id)
                } catch (e: Exception) {
                    Logger.withTag(TAG).w(e) { "Error removing download $id" }
                }
                activeDownloadId = null
            }
        }

        fun deleteModel(): Boolean {
            cancelDownload()
            val file = getModelFile() ?: return false
            return try {
                file.delete()
            } catch (e: Exception) {
                Logger.withTag(TAG).w(e) { "Error deleting model file" }
                false
            }
        }
    }
