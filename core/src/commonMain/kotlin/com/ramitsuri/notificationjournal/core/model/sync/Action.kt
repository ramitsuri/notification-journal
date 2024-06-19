package com.ramitsuri.notificationjournal.core.model.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Action {
    @SerialName("create")
    CREATE,

    @SerialName("update")
    UPDATE,

    @SerialName("delete")
    DELETE,
    ;
}
