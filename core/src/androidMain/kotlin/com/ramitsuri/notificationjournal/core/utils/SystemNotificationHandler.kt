package com.ramitsuri.notificationjournal.core.utils

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.app.TaskStackBuilder

class SystemNotificationHandler(
    context: Context,
) : NotificationHandler {
    private val appContext = context.applicationContext
    private val notificationManager = NotificationManagerCompat.from(appContext)

    override fun init(channels: List<NotificationChannelInfo>) {
        createChannels(channels)
    }

    override fun showNotification(notificationInfo: NotificationInfo) {
        val systemNotification = toSystemNotification(notificationInfo)
        val notificationId = getNotificationId(notificationInfo)
        if (ActivityCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationManager.notify(notificationId, systemNotification)
    }

    override fun getNotificationId(notificationInfo: NotificationInfo): Int {
        return when (notificationInfo.channel) {
            NotificationChannelType.MAIN -> 1
        }
    }

    private fun toSystemNotification(notificationInfo: NotificationInfo): Notification {
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
                            notificationInfo.actionExtras,
                        ),
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
            val contentIntent =
                TaskStackBuilder.create(appContext).run {
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
        notificationManager.deleteNotificationChannel("foreground_service")
        notificationManager.deleteNotificationChannel("app_restart")
    }

    private fun toNotificationChannel(notificationChannelInfo: NotificationChannelInfo): NotificationChannel {
        val importance = toImportance(notificationChannelInfo.importance)
        return NotificationChannel(
            notificationChannelInfo.channelType.id,
            notificationChannelInfo.name,
            importance,
        ).apply {
            description = notificationChannelInfo.description
            if (notificationChannelInfo.playSound) {
                val attributes =
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(soundUri, attributes)
            }
            vibrationPattern =
                if (notificationChannelInfo.vibrate) {
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
        actionExtras: Map<String, Any>?,
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
        val pendingIntent =
            PendingIntent.getBroadcast(
                appContext,
                actionRequestCode,
                intent,
                PendingIntent.FLAG_MUTABLE,
            )
        val resultKey = actionInfo.remoteInputKey
        val remoteInput: RemoteInput? =
            if (resultKey != null) {
                RemoteInput.Builder(resultKey).build()
            } else {
                null
            }
        return NotificationCompat.Action.Builder(
            0,
            actionInfo.text,
            pendingIntent,
        ).addRemoteInput(remoteInput).build()
    }

    private fun toImportance(importance: Importance) = importance.key
}
