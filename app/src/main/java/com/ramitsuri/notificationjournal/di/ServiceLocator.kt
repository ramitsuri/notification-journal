package com.ramitsuri.notificationjournal.di

import android.app.Application
import android.content.Context
import com.google.android.gms.wearable.Wearable
import com.ramitsuri.notificationjournal.BuildConfig
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.DataSharingClient
import com.ramitsuri.notificationjournal.core.data.DataSharingClientImpl
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.data.TagsDao
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
            dao = AppDatabase.getJournalEntryDao(applicationContext)
        )
    }

    val notificationHandler: NotificationHandler by lazy {
        SystemNotificationHandler(applicationContext)
    }

    val keyValueStore: KeyValueStore by lazy {
        PrefsKeyValueStore(applicationContext, Constants.PREF_FILE)
    }

    val tagsDao: TagsDao by lazy {
        AppDatabase.getTagsDao(applicationContext)
    }

    val templatesDao: JournalEntryTemplateDao by lazy {
        AppDatabase.getJournalEntryTemplateDao(applicationContext)
    }

    val dataSharingClient: DataSharingClient by lazy {
        DataSharingClientImpl(Wearable.getDataClient(applicationContext))
    }

    private val api: Api by lazy {
        buildApi(
            keyValueStore.getString(Constants.PREF_KEY_API_URL, Constants.DEFAULT_API_URL)
                ?: Constants.DEFAULT_API_URL,
            BuildConfig.DEBUG,
        )
    }

    private lateinit var applicationContext: Context
}