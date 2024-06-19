package com.ramitsuri.notificationjournal.core.network

import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import kotlinx.coroutines.Dispatchers

interface DataReceiveHelper {
    suspend fun startListening(onPayloadReceived: (Payload) -> Unit)

    suspend fun closeConnection()


    companion object {
        fun getDefault(): DataReceiveHelper = DataReceiveHelperImpl(
            ioDispatcher = Dispatchers.IO,
            hostName = "192.168.12.141",
            exchangeName = "TestExchange",
            clientName = ServiceLocator.getDeviceName(),
            clientId = ServiceLocator.getDeviceName(),
            json = ServiceLocator.json,
        )
    }
}