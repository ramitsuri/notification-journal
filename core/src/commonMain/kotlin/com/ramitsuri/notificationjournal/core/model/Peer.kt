package com.ramitsuri.notificationjournal.core.model

import com.ramitsuri.notificationjournal.core.model.sync.Sender
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class Peer(
    val id: String,
    val name: String,
    val lastSeenTime: Instant,
) {
    companion object {
        fun fromSender(
            sender: Sender,
            time: Instant = Clock.System.now(),
        ): Peer {
            return Peer(
                id = sender.id,
                name = sender.name,
                lastSeenTime = time,
            )
        }
    }
}
