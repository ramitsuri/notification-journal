package com.ramitsuri.notificationjournal.core.network

import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Action
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import kotlinx.coroutines.Dispatchers

interface DataSendHelper {

    suspend fun sendEntry(entry: JournalEntry, action: Action): Boolean

    suspend fun sendTags(tags: List<Tag>): Boolean

    suspend fun sendTemplates(templates: List<JournalEntryTemplate>): Boolean

    suspend fun closeConnection()

    companion object {
        fun getDefault(): DataSendHelper = DataSendHelperImpl(
            ioDispatcher = Dispatchers.IO,
            hostName = "192.168.12.141",
            exchangeName = "TestExchange",
            clientName = ServiceLocator.getDeviceName(),
            clientId = ServiceLocator.getDeviceName(),
            json = ServiceLocator.json,
        )
    }
}