package com.ramitsuri.notificationjournal.di

import android.app.Application
import android.content.Context
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.network.Api
import com.ramitsuri.notificationjournal.core.network.buildApi
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.KeyValueStore
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelInfo
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelType
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.ramitsuri.notificationjournal.core.utils.PrefsKeyValueStore
import com.ramitsuri.notificationjournal.core.utils.SystemNotificationHandler

object ServiceLocator {
    fun init(application: Application) {
        applicationContext = application
        notificationHandler.init(
            listOf(
                NotificationChannelInfo(
                    channelType = NotificationChannelType.MAIN,
                    name = NotificationChannelType.MAIN.id,
                    description = "For main notification"
                )
            )
        )
    }

    val repository: JournalRepository by lazy {
        JournalRepository(
            api = api,
            dao = AppDatabase.getDao(applicationContext)
        )
    }

    val notificationHandler: NotificationHandler by lazy {
        SystemNotificationHandler(applicationContext)
    }

    val keyValueStore: KeyValueStore by lazy {
        PrefsKeyValueStore(applicationContext, Constants.PREF_FILE)
    }

    private val api: Api by lazy {
        buildApi(
            keyValueStore.getString(Constants.PREF_KEY_API_URL, Constants.DEFAULT_API_URL)
                ?: Constants.DEFAULT_API_URL,
            Api::class.java
        )
    }

    private lateinit var applicationContext: Context
}