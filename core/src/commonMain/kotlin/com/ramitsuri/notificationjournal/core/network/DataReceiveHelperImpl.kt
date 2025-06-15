package com.ramitsuri.notificationjournal.core.network

import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn

internal class DataReceiveHelperImpl(
    ioDispatcher: CoroutineDispatcher,
    getDataHostProperties: suspend () -> DataHostProperties,
    rabbitMqHelper: RabbitMqHelper,
) : DataReceiveHelper {
    override val payloadFlow =
        rabbitMqHelper
            .receive(getDataHostProperties)
            .flowOn(ioDispatcher)
            .shareIn(
                scope = ServiceLocator.coroutineScope,
                started = WhileSubscribed(),
            )
}
