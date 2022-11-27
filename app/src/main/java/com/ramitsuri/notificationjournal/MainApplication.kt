package com.ramitsuri.notificationjournal

import android.app.Application
import com.ramitsuri.notificationjournal.broadcast.NotificationContentReceiver
import com.ramitsuri.notificationjournal.service.ForegroundService
import com.ramitsuri.notificationjournal.ui.MainActivity
import com.google.android.material.color.DynamicColors
import com.ramitsuri.notificationjournal.data.AppDatabase
import com.ramitsuri.notificationjournal.network.Api
import com.ramitsuri.notificationjournal.network.buildApi
import com.ramitsuri.notificationjournal.ui.MainViewModel
import com.ramitsuri.notificationjournal.utils.Constants
import com.ramitsuri.notificationjournal.utils.NotificationActionInfo
import com.ramitsuri.notificationjournal.utils.NotificationChannelInfo
import com.ramitsuri.notificationjournal.utils.NotificationChannelType
import com.ramitsuri.notificationjournal.utils.NotificationHandler
import com.ramitsuri.notificationjournal.utils.NotificationInfo
import com.ramitsuri.notificationjournal.utils.PrefsKeyValueStore
import com.ramitsuri.notificationjournal.utils.SystemNotificationHandler

class MainApplication : Application() {

    lateinit var notificationHandler: NotificationHandler
        private set

    override fun onCreate() {
        super.onCreate()

        DynamicColors.applyToActivitiesIfAvailable(this)
        start()
    }

    fun start() {
        notificationHandler = SystemNotificationHandler(this)
        notificationHandler.init(
            listOf(
                NotificationChannelInfo(
                    channelType = NotificationChannelType.FOREGROUND_SERVICE,
                    name = NotificationChannelType.FOREGROUND_SERVICE.id,
                    description = "For foreground service notification"
                ),
                NotificationChannelInfo(
                    channelType = NotificationChannelType.MAIN,
                    name = NotificationChannelType.MAIN.id,
                    description = "For main notification"
                )
            )
        )

        ForegroundService.start(this)
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
                    intentReceiverClass = NotificationContentReceiver::class.java,
                    remoteInputKey = Constants.REMOTE_INPUT_JOURNAL_KEY
                )
            ),
            actionExtras = mapOf()
        )
        notificationHandler.showNotification(notificationInfo)
    }

    fun getViewModelFactory(): MainViewModel.Factory {
        val keyValueStore = PrefsKeyValueStore(this, Constants.PREF_FILE)
        return MainViewModel.Factory(
            AppDatabase.getInstance(applicationContext).journalEntryDao(),
            keyValueStore,
            buildApi(
                keyValueStore.getString(Constants.PREF_KEY_API_URL, Constants.DEFAULT_API_URL)
                    ?: Constants.DEFAULT_API_URL,
                Api::class.java
            )
        )
    }
}