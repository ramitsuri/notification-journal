package com.ramitsuri.notificationjournal.core.data

import android.annotation.SuppressLint
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.ramitsuri.notificationjournal.core.utils.Constants
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

interface DataSharingClient {
    suspend fun postJournalEntry(value: String, time: Instant, timeZoneId: ZoneId): Boolean
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
            false
        } catch (exception: Exception) {
            false
        }
    }
}