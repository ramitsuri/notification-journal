package com.ramitsuri.notificationjournal.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.ramitsuri.notificationjournal.MainApplication
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.ui.MainActivity
import com.ramitsuri.notificationjournal.utils.Constants
import com.ramitsuri.notificationjournal.utils.NotificationChannelType
import com.ramitsuri.notificationjournal.utils.NotificationInfo

class ForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        super.onStartCommand(intent, flags, startId)

        val notificationInfo = NotificationInfo(
            channel = NotificationChannelType.FOREGROUND_SERVICE,
            title = "Foreground Service",
            body = "",
            iconResId = R.drawable.ic_notification,
            isVisibilityPublic = true,
            cancelOnTouch = false,
            intentClass = MainActivity::class.java,
            isForegroundServiceImmediate = true
        )
        val notificationHandler = (applicationContext as MainApplication).notificationHandler
        val systemNotification = notificationHandler.toSystemNotification(notificationInfo)
        val notificationId = notificationHandler.getNotificationId(notificationInfo)
        startForeground(notificationId, systemNotification)
        return START_STICKY
    }

    companion object {

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, ForegroundService::class.java)
                )
            } catch (e: ForegroundServiceStartNotAllowedException) {
                showAppRestartNotification(context = context)
            }
        }

        private fun showAppRestartNotification(context: Context) {
            val notificationInfo = NotificationInfo(
                channel = NotificationChannelType.APP_RESTART,
                title = "Tap to restart app",
                body = "",
                iconResId = R.drawable.ic_notification,
                isVisibilityPublic = true,
                cancelOnTouch = true,
                intentClass = MainActivity::class.java,
                intentExtras = mapOf(Constants.INTENT_EXTRA_IS_FROM_RESTART to true)
            )
            (context.applicationContext as MainApplication).notificationHandler.showNotification(
                notificationInfo
            )
        }

    }

}