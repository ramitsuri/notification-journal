package com.ramitsuri.notificationjournal.core.network

import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn

internal class DataReceiveHelperImpl(
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val getDataHostProperties: suspend () -> DataHostProperties,
    private val rabbitMqHelper: RabbitMqHelper,
) : DataReceiveHelper {
    override var payloadFlow = getFlow()
        private set

    override fun reset() {
        payloadFlow = getFlow()
    }

    private fun getFlow() =
        rabbitMqHelper
            .receive(getDataHostProperties)
            .flowOn(ioDispatcher)
            .shareIn(
                scope = coroutineScope,
                started = WhileSubscribed(5_000),
            )
}
