package com.ramitsuri.notificationjournal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WindowPosition(
    @SerialName("x")
    val x: Float,
    @SerialName("y")
    val y: Float,
)
