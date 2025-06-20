package com.ramitsuri.notificationjournal.core.network

import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn

internal class DataReceiveHelperImpl(
    coroutineScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
    getDataHostProperties: suspend () -> DataHostProperties,
    rabbitMqHelper: RabbitMqHelper,
) : DataReceiveHelper {
    override val payloadFlow =
        rabbitMqHelper
            .receive(getDataHostProperties)
            .flowOn(ioDispatcher)
            .shareIn(
                scope = coroutineScope,
                started = WhileSubscribed(5_000),
            )
}
