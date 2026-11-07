package com.ramitsuri.notificationjournal.core.data

import android.annotation.SuppressLint
import android.app.Application
import co.touchlab.kermit.Logger
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.utils.Constants
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.LocalDateTime
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class WearDataSharingClientImpl(
    application: Application,
) : WearDataSharingClient {
    private val dataClient: DataClient = Wearable.getDataClient(application)

    @SuppressLint("VisibleForTests")
    override suspend fun postJournalEntry(
        value: String,
        time: LocalDateTime,
        tag: String?,
    ): Boolean =
        try {
            val id = UUID.randomUUID().toString()
            val path = "${Constants.WearDataSharing.JOURNAL_ENTRY_ROUTE}/$id"
            val request =
                PutDataMapRequest
                    .create(path)
                    .apply {
                        dataMap.putString(Constants.WearDataSharing.JOURNAL_ENTRY_VALUE, value)
                        dataMap.putString(
                            Constants.WearDataSharing.JOURNAL_ENTRY_TIME,
                            time.toString(),
                        )
                        if (tag != null) {
                            dataMap.putString(Constants.WearDataSharing.JOURNAL_ENTRY_TAG, tag)
                        }
                    }.asPutDataRequest()
                    .setUrgent()

            dataClient.putDataItem(request).await()
            true
        } catch (cancellationException: CancellationException) {
            Logger.withTag(TAG).d { "Post entry cancelled: ${cancellationException.message}" }
            false
        } catch (exception: Exception) {
            Logger.withTag(TAG).e(exception) { "Failed to post entry" }
            false
        }

    @SuppressLint("VisibleForTests")
    override suspend fun requestUpload() {
        try {
            val id = UUID.randomUUID().toString()
            // Even though there's only going to be one upload event, somehow not providing a unique
            // id every time, makes it work only the first time.
            // Receiving client should resolve receiving multiple upload events.
            val path = "${Constants.WearDataSharing.REQUEST_UPLOAD_ROUTE}/$id"
            val request =
                PutDataMapRequest
                    .create(path)
                    .asPutDataRequest()
                    .setUrgent()

            dataClient.putDataItem(request).await()
        } catch (exception: Exception) {
            Logger.withTag(TAG).e(exception) { "Failed to request upload" }
        }
    }

    @SuppressLint("VisibleForTests")
    override suspend fun postTemplate(template: JournalEntryTemplate): Boolean =
        try {
            val requestId = UUID.randomUUID().toString()
            val path = "${Constants.WearDataSharing.TEMPLATE_ROUTE}/$requestId"
            val request =
                PutDataMapRequest
                    .create(path)
                    .apply {
                        dataMap.apply {
                            putString(Constants.WearDataSharing.TEMPLATE_ID, template.id)
                            putString(Constants.WearDataSharing.TEMPLATE_VALUE, template.text)
                            putString(Constants.WearDataSharing.TEMPLATE_TAG, template.tag)
                            putString(
                                Constants.WearDataSharing.TEMPLATE_DISPLAY_TEXT,
                                template.displayText,
                            )
                            putString(
                                Constants.WearDataSharing.TEMPLATE_SHORT_DISPLAY_TEXT,
                                template.shortDisplayText,
                            )
                        }
                    }.asPutDataRequest()
                    .setUrgent()

            dataClient.putDataItem(request).await()
            true
        } catch (exception: Exception) {
            Logger.withTag(TAG).e(exception) { "Failed to post template" }
            false
        }

    override suspend fun clearTemplates(): Boolean =
        try {
            val requestId = UUID.randomUUID().toString()
            val path = "${Constants.WearDataSharing.CLEAR_TEMPLATES_ROUTE}/$requestId"
            val request =
                PutDataMapRequest
                    .create(path)
                    .asPutDataRequest()
                    .setUrgent()

            dataClient.putDataItem(request).await()
            true
        } catch (exception: Exception) {
            Logger.withTag(TAG).e(exception) { "Failed to clear templates" }
            false
        }

    override suspend fun updateTile(): Boolean =
        try {
            val requestId = UUID.randomUUID().toString()
            val path = "${Constants.WearDataSharing.UPDATE_TILE_ROUTE}/$requestId"
            val request =
                PutDataMapRequest
                    .create(path)
                    .asPutDataRequest()
                    .setUrgent()

            dataClient.putDataItem(request).await()
            true
        } catch (exception: Exception) {
            Logger.withTag(TAG).e(exception) { "Failed to update tile" }
            false
        }

    companion object {
        private const val TAG = "DataSharingClient"
    }
}
