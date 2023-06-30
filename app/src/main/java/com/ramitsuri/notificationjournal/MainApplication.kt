package com.ramitsuri.notificationjournal

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.ramitsuri.notificationjournal.broadcast.NotificationActionReceiver
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.network.Api
import com.ramitsuri.notificationjournal.core.network.buildApi
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.KeyValueStore
import com.ramitsuri.notificationjournal.core.utils.NotificationActionInfo
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelInfo
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelType
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.ramitsuri.notificationjournal.core.utils.NotificationInfo
import com.ramitsuri.notificationjournal.core.utils.PrefsKeyValueStore
import com.ramitsuri.notificationjournal.core.utils.SystemNotificationHandler
import com.ramitsuri.notificationjournal.ui.MainActivity
import com.ramitsuri.notificationjournal.ui.MainViewModel

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
        notificationHandler.showNotification(notificationInfo)
    }

    fun getViewModelFactory(): MainViewModel.Factory {
        return MainViewModel.Factory(
            getKeyValueStore(),
            getRepository()
        )
    }

    fun getRepository(): JournalRepository {
        return JournalRepository(
            api = getApi(),
            dao = AppDatabase.getDao(applicationContext)
        )
    }

    private fun getApi(): Api {
        return buildApi(
            getKeyValueStore().getString(Constants.PREF_KEY_API_URL, Constants.DEFAULT_API_URL)
                ?: Constants.DEFAULT_API_URL,
            Api::class.java
        )
    }

    private fun getKeyValueStore(): KeyValueStore {
        return PrefsKeyValueStore(this, Constants.PREF_FILE)
    }
}