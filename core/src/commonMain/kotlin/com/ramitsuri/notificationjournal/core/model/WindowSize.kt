package com.ramitsuri.notificationjournal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WindowSize(
    @SerialName("height")
    val height: Float,
    @SerialName("width")
    val width: Float,
)
