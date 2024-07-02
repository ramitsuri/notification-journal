package com.ramitsuri.notificationjournal.service

import android.annotation.SuppressLint
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.tile.JournalTileService
import kotlinx.coroutines.launch

class WearDataLayerListenerService : WearableListenerService() {
    @SuppressLint("VisibleForTests")
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val addTemplateEvents = mutableListOf<DataEvent>()
        dataEvents.forEach { event ->
            val path = event.dataItem.uri.path ?: ""
            if (path.startsWith(Constants.WearDataSharing.TEMPLATE_ROUTE)) {
                addTemplateEvents.add(event)
            }
        }

        if (addTemplateEvents.isEmpty()) {
            return
        }
        val journalEntryTemplates = mutableListOf<JournalEntryTemplate>()
        addTemplateEvents.forEach { dataEvent ->
            val dataMap = DataMapItem.fromDataItem(dataEvent.dataItem).dataMap
            val templateId = dataMap.getString(Constants.WearDataSharing.TEMPLATE_ID)
            val templateValue = dataMap.getString(Constants.WearDataSharing.TEMPLATE_VALUE)
            val templateTag = dataMap.getString(Constants.WearDataSharing.TEMPLATE_TAG)
            if (templateId != null && templateValue != null && templateTag != null) {
                val template = JournalEntryTemplate(
                    id = templateId,
                    text = templateValue,
                    tag = templateTag
                )
                journalEntryTemplates.add(template)
            }
        }

        val dao = ServiceLocator.templatesDao

        ServiceLocator.coroutineScope.launch {
            // Use ones received from the phone app as the single source of truth
            dao.clearAndInsert(journalEntryTemplates)
            JournalTileService.update(applicationContext)
        }
    }
}