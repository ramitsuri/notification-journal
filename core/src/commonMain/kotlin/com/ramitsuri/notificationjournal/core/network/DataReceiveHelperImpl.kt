package com.ramitsuri.notificationjournal.core.network

internal class DataReceiveHelperImpl(
    webSocketHelper: WebSocketHelper,
) : DataReceiveHelper {
    override val payloadFlow = webSocketHelper.messages
}
