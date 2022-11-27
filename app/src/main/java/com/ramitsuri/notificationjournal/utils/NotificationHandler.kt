package com.ramitsuri.notificationjournal.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.app.TaskStackBuilder

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
    FOREGROUND_SERVICE("foreground_service"),
    APP_RESTART("app_restart")
}

interface NotificationHandler {
    fun init(channels: List<NotificationChannelInfo>)

    fun showNotification(notificationInfo: NotificationInfo)

    fun toSystemNotification(notificationInfo: NotificationInfo): Notification

    fun getNotificationId(notificationInfo: NotificationInfo): Int
}

internal class SystemNotificationHandler(
    context: Context
) : NotificationHandler {
    private val appContext = context.applicationContext
    private val notificationManager = NotificationManagerCompat.from(appContext)

    override fun init(channels: List<NotificationChannelInfo>) {
        createChannels(channels)
    }

    override fun showNotification(notificationInfo: NotificationInfo) {
        val systemNotification = toSystemNotification(notificationInfo)
        val notificationId = getNotificationId(notificationInfo)
        notificationManager.notify(notificationId, systemNotification)
    }

    override fun getNotificationId(notificationInfo: NotificationInfo): Int {
        return when (notificationInfo.channel) {
            NotificationChannelType.MAIN -> 1

            NotificationChannelType.FOREGROUND_SERVICE -> 2

            NotificationChannelType.APP_RESTART -> 3
        }
    }

    override fun toSystemNotification(notificationInfo: NotificationInfo): Notification {
        val builder = NotificationCompat.Builder(appContext, notificationInfo.channel.id)
        builder.apply {
            setSmallIcon(notificationInfo.iconResId)
            if (notificationInfo.isVisibilityPublic) {
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            }
            if (notificationInfo.title.isNotEmpty()) {
                setContentTitle(notificationInfo.title)
            }
            if (notificationInfo.body.isNotEmpty()) {
                setContentText(notificationInfo.body)
            }
            notificationInfo.actions?.let {
                for ((index, action) in it.withIndex()) {
                    addAction(
                        getAction(
                            getNotificationId(notificationInfo),
                            index,
                            action,
                            notificationInfo.actionExtras
                        )
                    )
                }
            }
            if (notificationInfo.isOngoing) {
                setOngoing(true)
                setShowWhen(false)
                setWhen(0)
            } else {
                setOngoing(false)
            }
            setAutoCancel(notificationInfo.cancelOnTouch)
            val contentIntent = TaskStackBuilder.create(appContext).run {
                val mainIntent = Intent(appContext, notificationInfo.intentClass)
                addNextIntentWithParentStack(mainIntent)
                val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                getPendingIntent(1001, flags)
            }
            setContentIntent(contentIntent)

            if (notificationInfo.isForegroundServiceImmediate) {
                foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
            }
        }
        return builder.build()
    }

    private fun createChannels(channelInfos: List<NotificationChannelInfo>) {
        for (channelInfo in channelInfos) {
            notificationManager.createNotificationChannel(toNotificationChannel(channelInfo))
        }
    }

    private fun toNotificationChannel(notificationChannelInfo: NotificationChannelInfo): NotificationChannel {
        val importance = toImportance(notificationChannelInfo.importance)
        return NotificationChannel(
            notificationChannelInfo.channelType.id,
            notificationChannelInfo.name,
            importance
        ).apply {
            description = notificationChannelInfo.description
            if (notificationChannelInfo.playSound) {
                val attributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(soundUri, attributes)
            }
            vibrationPattern = if (notificationChannelInfo.vibrate) {
                longArrayOf(200)
            } else {
                null
            }
            setShowBadge(notificationChannelInfo.showBadge)
        }
    }

    private fun getAction(
        notificationId: Int,
        actionIndex: Int,
        actionInfo: NotificationActionInfo,
        actionExtras: Map<String, Any>?
    ): NotificationCompat.Action {
        val actionRequestCode = notificationId * 10 + actionIndex
        val intent = Intent(appContext, actionInfo.intentReceiverClass)
        intent.action = actionInfo.action
        actionExtras?.forEach { (extraKey, extraValue) ->
            when (extraValue) {
                is String -> {
                    intent.putExtra(extraKey, extraValue)
                }
                is Int -> {
                    intent.putExtra(extraKey, extraValue)
                }
                is Long -> {
                    intent.putExtra(extraKey, extraValue)
                }
                is Boolean -> {
                    intent.putExtra(extraKey, extraValue)
                }
            }
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            actionRequestCode,
            intent,
            PendingIntent.FLAG_MUTABLE
        )
        val remoteInput: RemoteInput? = if (actionInfo.remoteInputKey != null) {
            RemoteInput.Builder(actionInfo.remoteInputKey).build()
        } else {
            null
        }
        return NotificationCompat.Action.Builder(
            0,
            appContext.getString(actionInfo.textResId),
            pendingIntent
        ).addRemoteInput(remoteInput).build()
    }


    private fun toImportance(importance: Importance) = importance.key
}