package com.ramitsuri.notificationjournal.core.utils

import androidx.annotation.DrawableRes

data class NotificationChannelInfo(
    val channelType: NotificationChannelType,
    val name: String,
    val description: String,
    val importance: Importance = Importance.LOW,
    val playSound: Boolean = false,
    val vibrate: Boolean = false,
    val showBadge: Boolean = false
)

/**
 * @property isForegroundServiceImmediate Should set to true if the notification is for a foreground service and it
 * should be shown immediately.
 */
data class NotificationInfo(
    val channel: NotificationChannelType,
    val title: String,
    val body: String,
    @DrawableRes val iconResId: Int,
    val isVisibilityPublic: Boolean = true,
    val cancelOnTouch: Boolean = false,
    val isForegroundServiceImmediate: Boolean = false,
    val isOngoing: Boolean = false,
    val intentClass: Class<*>,
    val intentExtras: Map<String, Any>? = null,
    val actions: List<NotificationActionInfo>? = null,
    val actionExtras: Map<String, Any>? = null
)

data class NotificationActionInfo(
    val action: String,
    val textResId: Int,
    val intentReceiverClass: Class<*>,
    val remoteInputKey: String?
)

enum class Importance(val key: Int) {
    NONE(0),
    MIN(1),
    LOW(2),
    DEFAULT(3),
    HIGH(4),
    MAX(5);
}

enum class NotificationChannelType(val id: String) {
    MAIN("main"),
}

interface NotificationHandler {
    fun init(channels: List<NotificationChannelInfo>)

    fun showNotification(notificationInfo: NotificationInfo)

    fun getNotificationId(notificationInfo: NotificationInfo): Int
}
