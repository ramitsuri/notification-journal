package com.ramitsuri.notificationjournal.service

import android.annotation.SuppressLint
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

class PhoneDataLayerListenerService : WearableListenerService() {

    @SuppressLint("VisibleForTests")
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val addJournalEntryEvents = mutableListOf<DataEvent>()
        val uploadEvents = mutableListOf<DataEvent>()
        dataEvents.forEach { event ->
            val path = event.dataItem.uri.path ?: ""
            if (path.startsWith(Constants.DataSharing.JOURNAL_ENTRY_ROUTE)) {
                addJournalEntryEvents.add(event)
            } else if (path.startsWith(Constants.DataSharing.REQUEST_UPLOAD_ROUTE)) {
                uploadEvents.add(event)
            }
        }

        addJournalEntryEvents.forEach { dataEvent ->
            val dataMap = DataMapItem.fromDataItem(dataEvent.dataItem).dataMap
            val journalEntryText = dataMap.getString(Constants.DataSharing.JOURNAL_ENTRY_VALUE)
            if (journalEntryText != null) {
                val journalEntryTimeMillis =
                    dataMap.getLong(Constants.DataSharing.JOURNAL_ENTRY_TIME)
                val journalEntryTime = if (journalEntryTimeMillis == 0L) {
                    Clock.System.now()
                } else {
                    Instant.fromEpochMilliseconds(journalEntryTimeMillis)
                }

                val journalEntryTimeZoneId =
                    dataMap.getString(Constants.DataSharing.JOURNAL_ENTRY_TIME_ZONE)
                val journalEntryTimeZone = try {
                    if (journalEntryTimeZoneId != null) {
                        TimeZone.of(journalEntryTimeZoneId)
                    } else {
                        TimeZone.currentSystemDefault()
                    }
                } catch (e: Exception) {
                    TimeZone.currentSystemDefault()
                }
                val tag = dataMap.getString(Constants.DataSharing.JOURNAL_ENTRY_TAG)

                val journalEntry = JournalEntry(
                    id = 0,
                    entryTime = journalEntryTime,
                    timeZone = journalEntryTimeZone,
                    text = journalEntryText,
                    tag = tag,
                )

                val dao = ServiceLocator.journalEntryDao
                CoroutineScope(SupervisorJob()).launch {
                    withContext(Dispatchers.IO) {
                        dao.insert(journalEntry)
                    }
                }
            }
        }

        if (uploadEvents.isNotEmpty()) {
            CoroutineScope(SupervisorJob()).launch {
                val error = ServiceLocator.repository.upload() ?: return@launch
                Log.d(TAG, "Failed to upload: $error")
            }
        }
    }

    companion object {
        private const val TAG = "PhoneDataLayerListenerService"
    }
}
