package com.ramitsuri.notificationjournal.core.data

import android.annotation.SuppressLint
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.ramitsuri.notificationjournal.core.utils.Constants
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

interface DataSharingClient {
    suspend fun postJournalEntry(value: String, time: Instant, timeZoneId: ZoneId): Boolean

    suspend fun requestUpload()
}

class DataSharingClientImpl(
    private val dataClient: DataClient
) : DataSharingClient {
    @SuppressLint("VisibleForTests")
    override suspend fun postJournalEntry(
        value: String,
        time: Instant,
        timeZoneId: ZoneId
    ): Boolean {
        return try {
            val id = System.currentTimeMillis()
            val path = "${Constants.DataSharing.JOURNAL_ENTRY_ROUTE}/$id"
            val request = PutDataMapRequest.create(path)
                .apply {
                    dataMap.putString(Constants.DataSharing.JOURNAL_ENTRY_VALUE, value)
                    dataMap.putLong(Constants.DataSharing.JOURNAL_ENTRY_TIME, time.toEpochMilli())
                    dataMap.putString(Constants.DataSharing.JOURNAL_ENTRY_TIME_ZONE, timeZoneId.id)
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

    companion object {
        private const val TAG = "DataSharingClient"
    }
}