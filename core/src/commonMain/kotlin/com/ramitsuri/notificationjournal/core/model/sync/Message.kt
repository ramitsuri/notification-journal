package com.ramitsuri.notificationjournal.core.model.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Message {
    @Serializable
    @SerialName("data")
    data class Data(
        val id: String? = null,
        val payload: Payload,
    ) : Message

    @Serializable
    @SerialName("ack")
    data class Ack(
        val id: String,
    ) : Message
}
