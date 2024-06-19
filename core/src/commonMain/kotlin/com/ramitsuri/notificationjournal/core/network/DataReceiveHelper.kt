package com.ramitsuri.notificationjournal.core.network

import com.ramitsuri.notificationjournal.core.model.sync.Payload

interface DataReceiveHelper {
    suspend fun startListening(onPayloadReceived: (Payload) -> Unit)

    suspend fun closeConnection()
}