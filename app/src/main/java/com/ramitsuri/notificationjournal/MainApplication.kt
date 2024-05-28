package com.ramitsuri.notificationjournal

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.ramitsuri.notificationjournal.broadcast.NotificationActionReceiver
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.utils.NotificationActionInfo
import com.ramitsuri.notificationjournal.utils.NotificationChannelType
import com.ramitsuri.notificationjournal.utils.NotificationInfo
import com.ramitsuri.notificationjournal.di.ServiceLocator

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        DynamicColors.applyToActivitiesIfAvailable(this)
        ServiceLocator.init(this)
        showJournalNotification()
    }

    fun showJournalNotification() {
        val notificationInfo = NotificationInfo(
            channel = NotificationChannelType.MAIN,
            title = "",
            body = "",
            iconResId = R.drawable.ic_notification,
            isVisibilityPublic = true,
            cancelOnTouch = false,
            isForegroundServiceImmediate = false,
            isOngoing = true,
            intentClass = MainActivity::class.java,
            intentExtras = mapOf(),
            actions = listOf(
                NotificationActionInfo(
                    action = Constants.ACTION_JOURNAL,
                    textResId = R.string.add_new_journal_content,
                    intentReceiverClass = NotificationActionReceiver::class.java,
                    remoteInputKey = Constants.REMOTE_INPUT_JOURNAL_KEY
                ),
                NotificationActionInfo(
                    action = Constants.ACTION_UPLOAD,
                    textResId = R.string.upload_journal_content,
                    intentReceiverClass = NotificationActionReceiver::class.java,
                    remoteInputKey = null
                )
            ),
            actionExtras = mapOf()
        )
        ServiceLocator.notificationHandler.showNotification(notificationInfo)
    }
}