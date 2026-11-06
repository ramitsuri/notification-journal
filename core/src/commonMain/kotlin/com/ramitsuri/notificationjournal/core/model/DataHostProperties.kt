package com.ramitsuri.notificationjournal.core.model

import com.ramitsuri.notificationjournal.core.model.sync.Sender
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DataHostProperties(
    @SerialName("deviceName")
    val deviceName: String = "",
    @SerialName("deviceId")
    val deviceId: String = "",
    @SerialName("dataHost")
    val dataHost: String = "http://",
    @SerialName("otherHosts")
    val otherHosts: Set<String> = emptySet(),
) {
    fun isValid(): Boolean = deviceName.isNotBlank() && deviceId.isNotBlank() && dataHost.isNotBlank()
}

fun DataHostProperties.toSender(): Sender =
    Sender(
        name = deviceName,
        id = deviceId,
    )
