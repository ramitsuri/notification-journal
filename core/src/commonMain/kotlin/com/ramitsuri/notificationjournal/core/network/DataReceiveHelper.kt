package com.ramitsuri.notificationjournal.core.network

import com.ramitsuri.notificationjournal.core.model.sync.Payload
import kotlinx.coroutines.flow.SharedFlow

interface DataReceiveHelper {
    val payloadFlow: SharedFlow<Payload>

    fun reset()
}
