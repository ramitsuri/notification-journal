package com.ramitsuri.notificationjournal.service

import android.annotation.SuppressLint
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WearDataLayerListenerService : WearableListenerService() {
    @SuppressLint("VisibleForTests")
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val addTemplateEvents = mutableListOf<DataEvent>()
        dataEvents.forEach { event ->
            val path = event.dataItem.uri.path ?: ""
            if (path.startsWith(Constants.DataSharing.TEMPLATE_ROUTE)) {
                addTemplateEvents.add(event)
            }
        }

        if (addTemplateEvents.isEmpty()) {
            return
        }
        val journalEntryTemplates = mutableListOf<JournalEntryTemplate>()
        addTemplateEvents.forEach { dataEvent ->
            val dataMap = DataMapItem.fromDataItem(dataEvent.dataItem).dataMap
            val templateId = dataMap.getInt(Constants.DataSharing.TEMPLATE_ID)
            val templateValue = dataMap.getString(Constants.DataSharing.TEMPLATE_VALUE)
            val templateTag = dataMap.getString(Constants.DataSharing.TEMPLATE_TAG)
            if (templateId != 0 && templateValue != null && templateTag != null) {
                val template = JournalEntryTemplate(
                    id = templateId,
                    text = templateValue,
                    tag = templateTag
                )
                journalEntryTemplates.add(template)
            }
        }

        val coroutineScope = CoroutineScope(SupervisorJob())
        val dao = ServiceLocator.templatesDao

        coroutineScope.launch {
            // Use ones received from the phone app as the single source of truth
            dao.deleteAll()
            journalEntryTemplates.forEach { template ->
                dao.insert(template)
            }
        }
    }
}