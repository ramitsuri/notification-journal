package com.ramitsuri.notificationjournal.service

import android.annotation.SuppressLint
import android.app.Service
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.ramitsuri.notificationjournal.core.di.AppScope
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.nowLocal
import com.ramitsuri.notificationjournal.work.UploadWorker
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ServiceKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlin.time.Clock

@Inject
@ContributesIntoMap(AppScope::class, binding = binding<Service>())
@ServiceKey
class PhoneDataLayerListenerService(
    private val repository: JournalRepository,
    private val coroutineScope: CoroutineScope,
) : WearableListenerService() {
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
                val journalEntryTime =
                    dataMap
                        .getString(Constants.WearDataSharing.JOURNAL_ENTRY_TIME)
                        ?.let { LocalDateTime.parse(it) }
                        ?: Clock.System.nowLocal()

                val tag = dataMap.getString(Constants.WearDataSharing.JOURNAL_ENTRY_TAG)

                coroutineScope.launch {
                    repository.insert(
                        text = journalEntryText,
                        time = journalEntryTime,
                        tag = tag,
                    )
                }
            }
        }

        if (uploadEvents.isNotEmpty()) {
            UploadWorker.enqueueImmediate(this)
        }
    }
}
