package com.ramitsuri.notificationjournal.service

import android.annotation.SuppressLint
import android.util.Log
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
        val clearTemplatesEvent = mutableListOf<DataEvent>()
        val updateTileEvent = mutableListOf<DataEvent>()
        dataEvents.forEach { event ->
            val path = event.dataItem.uri.path ?: ""
            if (path.startsWith(Constants.WearDataSharing.TEMPLATE_ROUTE)) {
                addTemplateEvents.add(event)
            }
            if (path.startsWith(Constants.WearDataSharing.CLEAR_TEMPLATES_ROUTE)) {
                clearTemplatesEvent.add(event)
            }
            if (path.startsWith(Constants.WearDataSharing.UPDATE_TILE_ROUTE)) {
                updateTileEvent.add(event)
            }
        }

        val dao = ServiceLocator.templatesDao

        val templates = addTemplateEvents.mapNotNull { it.toTemplate() }
        val delete = clearTemplatesEvent.isNotEmpty()
        val insert = addTemplateEvents.isNotEmpty()
        val updateTile = updateTileEvent.isNotEmpty()

        ServiceLocator.coroutineScope.launch {
            if (delete) {
                log("Deleting existing templates")
                dao.deleteAll()
            }
            if (insert) {
                log("Inserting ${templates.size} templates")
                dao.insert(templates)
            }
            if (updateTile) {
                log("Updating tile")
                JournalTileService.update(applicationContext)
            }
        }
    }

    private fun DataEvent.toTemplate(): JournalEntryTemplate? {
        val dataMap = DataMapItem.fromDataItem(this.dataItem).dataMap
        val templateId = dataMap.getString(Constants.WearDataSharing.TEMPLATE_ID)
        val templateValue = dataMap.getString(Constants.WearDataSharing.TEMPLATE_VALUE)
        val templateTag = dataMap.getString(Constants.WearDataSharing.TEMPLATE_TAG)
        val templateDisplayText =
            dataMap.getString(Constants.WearDataSharing.TEMPLATE_DISPLAY_TEXT)
        val templateShortDisplayText =
            dataMap.getString(Constants.WearDataSharing.TEMPLATE_SHORT_DISPLAY_TEXT)
        return if (templateId != null && templateValue != null && templateTag != null &&
            templateDisplayText != null && templateShortDisplayText != null
        ) {
            JournalEntryTemplate(
                id = templateId,
                text = templateValue,
                tag = templateTag,
                displayText = templateDisplayText,
                shortDisplayText = templateShortDisplayText,
            )
        } else {
            null
        }
    }

    private fun log(message: String) {
        Log.d("WearDataLayerListenerService", message)
    }
}
