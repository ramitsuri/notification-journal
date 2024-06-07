package com.ramitsuri.notificationjournal.core.data

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.ramitsuri.notificationjournal.core.utils.Constants
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.coroutines.cancellation.CancellationException

class DataSharingClientImpl(
    application: Application,
) : DataSharingClient {

    private val dataClient: DataClient = Wearable.getDataClient(application)

    @SuppressLint("VisibleForTests")
    override suspend fun postJournalEntry(
        value: String,
        time: Instant,
        timeZoneId: TimeZone,
        tag: String?,
    ): Boolean {
        return try {
            val id = System.currentTimeMillis()
            val path = "${Constants.DataSharing.JOURNAL_ENTRY_ROUTE}/$id"
            val request = PutDataMapRequest.create(path)
                .apply {
                    dataMap.putString(Constants.DataSharing.JOURNAL_ENTRY_VALUE, value)
                    dataMap.putLong(
                        Constants.DataSharing.JOURNAL_ENTRY_TIME,
                        time.toEpochMilliseconds()
                    )
                    dataMap.putString(Constants.DataSharing.JOURNAL_ENTRY_TIME_ZONE, timeZoneId.id)
                    if (tag != null) {
                        dataMap.putString(Constants.DataSharing.JOURNAL_ENTRY_TAG, tag)
                    }
                }
                .asPutDataRequest()
                .setUrgent()

            dataClient.putDataItem(request).await()
            true
        } catch (cancellationException: CancellationException) {
            Log.d(TAG, "Failed to post entry: ${cancellationException.message}")
            false
        } catch (exception: Exception) {
            Log.d(TAG, "Failed to post entry: ${exception.message}")
            false
        }
    }

    @SuppressLint("VisibleForTests")
    override suspend fun requestUpload() {
        try {
            val id = System.currentTimeMillis()
            // Even though there's only going to be one upload event, somehow not providing a unique
            // id every time, makes it work only the first time.
            // Receiving client should resolve receiving multiple upload events.
            val path = "${Constants.DataSharing.REQUEST_UPLOAD_ROUTE}/$id"
            val request = PutDataMapRequest.create(path)
                .asPutDataRequest()
                .setUrgent()

            dataClient.putDataItem(request).await()
        } catch (exception: Exception) {
            Log.d(TAG, "Failed to request upload: ${exception.message}")
        }
    }

    @SuppressLint("VisibleForTests")
    override suspend fun postTemplate(id: String, value: String, tag: String): Boolean {
        return try {
            val requestId = System.currentTimeMillis()
            val path = "${Constants.DataSharing.TEMPLATE_ROUTE}/$requestId"
            val request = PutDataMapRequest.create(path)
                .apply {
                    dataMap.putString(Constants.DataSharing.TEMPLATE_ID, id)
                    dataMap.putString(Constants.DataSharing.TEMPLATE_VALUE, value)
                    dataMap.putString(Constants.DataSharing.TEMPLATE_TAG, tag)
                }
                .asPutDataRequest()
                .setUrgent()

            dataClient.putDataItem(request).await()
            true
        } catch (exception: Exception) {
            Log.d(TAG, "Failed to post template: ${exception.message}")
            false
        }
    }

    companion object {
        private const val TAG = "DataSharingClient"
    }
}