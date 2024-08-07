package com.ramitsuri.notificationjournal.service

import android.annotation.SuppressLint
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.utils.Constants
import kotlinx.coroutines.launch
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
            if (path.startsWith(Constants.WearDataSharing.JOURNAL_ENTRY_ROUTE)) {
                addJournalEntryEvents.add(event)
            } else if (path.startsWith(Constants.WearDataSharing.REQUEST_UPLOAD_ROUTE)) {
                uploadEvents.add(event)
            }
        }

        addJournalEntryEvents.forEach { dataEvent ->
            val dataMap = DataMapItem.fromDataItem(dataEvent.dataItem).dataMap
            val journalEntryText = dataMap.getString(Constants.WearDataSharing.JOURNAL_ENTRY_VALUE)
            if (!journalEntryText.isNullOrEmpty()) {
                val journalEntryTimeMillis =
                    dataMap.getLong(Constants.WearDataSharing.JOURNAL_ENTRY_TIME)
                val journalEntryTime = if (journalEntryTimeMillis == 0L) {
                    Clock.System.now()
                } else {
                    Instant.fromEpochMilliseconds(journalEntryTimeMillis)
                }

                val journalEntryTimeZoneId =
                    dataMap.getString(Constants.WearDataSharing.JOURNAL_ENTRY_TIME_ZONE)
                val journalEntryTimeZone = try {
                    if (journalEntryTimeZoneId != null) {
                        TimeZone.of(journalEntryTimeZoneId)
                    } else {
                        TimeZone.currentSystemDefault()
                    }
                } catch (e: Exception) {
                    TimeZone.currentSystemDefault()
                }
                val tag = dataMap.getString(Constants.WearDataSharing.JOURNAL_ENTRY_TAG)

                val repository = ServiceLocator.repository
                ServiceLocator.coroutineScope.launch {
                    repository.insert(
                        text = journalEntryText,
                        time = journalEntryTime,
                        tag = tag,
                        timeZone = journalEntryTimeZone
                    )
                }
            }
        }

        if (uploadEvents.isNotEmpty()) {
            ServiceLocator.coroutineScope.launch {
                ServiceLocator.repository.sync()
            }
        }
    }
}
