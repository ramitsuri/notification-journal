package com.ramitsuri.notificationjournal.core.model.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Sender(
    @SerialName("name")
    val name: String,

    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),
)
