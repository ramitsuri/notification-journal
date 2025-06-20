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
    @SerialName("exchangeName")
    val exchangeName: String = "",
    @SerialName("dataHost")
    val dataHost: String = "http://",
    @SerialName("username")
    val username: String = "",
    @SerialName("password")
    val password: String = "",
    @SerialName("otherHosts")
    val otherHosts: Set<String> = emptySet(),
) {
    fun isValid(): Boolean {
        return deviceName.isNotBlank() && deviceId.isNotBlank() && exchangeName.isNotBlank() &&
            dataHost.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    }
}

fun DataHostProperties.toSender(): Sender {
    return Sender(
        name = deviceName,
        id = deviceId,
    )
}
