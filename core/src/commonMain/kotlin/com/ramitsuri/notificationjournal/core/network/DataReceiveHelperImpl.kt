package com.ramitsuri.notificationjournal.core.network

import com.ramitsuri.notificationjournal.core.model.sync.Entity

internal class DataReceiveHelperImpl(
    private val webSocketHelper: WebSocketHelper,
) : DataReceiveHelper {
    override val payloadFlow = webSocketHelper.messages

    override fun setEntityHandler(handler: suspend (Entity) -> Unit) {
        webSocketHelper.setEntityHandler(handler)
    }
}
