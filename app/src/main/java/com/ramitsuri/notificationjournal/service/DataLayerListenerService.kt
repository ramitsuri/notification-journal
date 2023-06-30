package com.ramitsuri.notificationjournal.service

import android.annotation.SuppressLint
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.JournalEntry
import com.ramitsuri.notificationjournal.core.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

class DataLayerListenerService : WearableListenerService() {

    @SuppressLint("VisibleForTests")
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { dataEvent ->
            if (dataEvent.dataItem.uri.path == Constants.DataSharing.JOURNAL_ENTRY_ROUTE) {
                val dataMap = DataMapItem.fromDataItem(dataEvent.dataItem).dataMap
                val journalEntryText = dataMap.getString(Constants.DataSharing.JOURNAL_ENTRY_VALUE)
                if (journalEntryText != null) {
                    val journalEntryTimeMillis =
                        dataMap.getLong(Constants.DataSharing.JOURNAL_ENTRY_TIME)
                    val journalEntryTime = if (journalEntryTimeMillis == 0L) {
                        Instant.now()
                    } else {
                        Instant.ofEpochMilli(journalEntryTimeMillis)
                    }

                    val journalEntryTimeZoneId =
                        dataMap.getString(Constants.DataSharing.JOURNAL_ENTRY_TIME_ZONE)
                    val journalEntryTimeZone = try {
                        ZoneId.of(journalEntryTimeZoneId)
                    } catch (e: Exception) {
                        ZoneId.systemDefault()
                    }

                    val journalEntry = JournalEntry(
                        id = 0,
                        entryTime = journalEntryTime,
                        timeZone = journalEntryTimeZone,
                        text = journalEntryText

                    )

                    val dao = AppDatabase.getDao(context = applicationContext)
                    CoroutineScope(SupervisorJob()).launch {
                        withContext(Dispatchers.IO) {
                            dao.insert(journalEntry)
                        }
                    }
                }
            }
        }
    }
}
