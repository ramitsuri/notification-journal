package com.ramitsuri.notificationjournal.core.di

import androidx.navigation.NavBackStackEntry
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.WearDataSharingClient
import com.ramitsuri.notificationjournal.core.data.JournalEntryDao
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.network.Api
import com.ramitsuri.notificationjournal.core.network.DataSendHelper
import com.ramitsuri.notificationjournal.core.network.buildApi
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.ui.addjournal.AddJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.editjournal.EditJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.KeyValueStore
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelInfo
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelType
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.ramitsuri.notificationjournal.core.utils.PrefsKeyValueStore
import kotlinx.serialization.json.Json

object ServiceLocator {
    fun init(factory: Factory) {
        ServiceLocator.factory = factory
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
            dao = AppDatabase.getJournalEntryDao(factory)
        )
    }

    val notificationHandler: NotificationHandler by lazy {
        factory.getNotificationHandler()
    }

    val keyValueStore: KeyValueStore by lazy {
        PrefsKeyValueStore(factory)
    }

    val journalEntryDao: JournalEntryDao by lazy {
        AppDatabase.getJournalEntryDao(factory)
    }

    val tagsDao: TagsDao by lazy {
        AppDatabase.getTagsDao(factory)
    }

    val templatesDao: JournalEntryTemplateDao by lazy {
        AppDatabase.getJournalEntryTemplateDao(factory)
    }

    val wearDataSharingClient: WearDataSharingClient by lazy {
        factory.getWearDataSharingClient()
    }

    val json by lazy {
        Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        }
    }

    fun getAddJournalEntryVMFactory(
        navBackStackEntry: NavBackStackEntry
    ) = factory.addJournalEntryVMFactory(
        navBackStackEntry,
    ) { savedStateHandle ->
        AddJournalEntryViewModel(
            savedStateHandle = savedStateHandle,
            repository = repository,
            tagsDao = tagsDao,
            templatesDao = templatesDao,
        )
    }

    fun getEditJournalEntryVMFactory(
        navBackStackEntry: NavBackStackEntry
    ) = factory.editJournalEntryVMFactory(
        navBackStackEntry,
    ) { savedStateHandle ->
        EditJournalEntryViewModel(
            savedStateHandle = savedStateHandle,
            repository = repository,
            tagsDao = tagsDao,
        )
    }

    fun getDeviceName() = factory.deviceName()

    val dataSendHelper by lazy {
        DataSendHelper.getDefault()
    }

    private val api: Api by lazy {
        buildApi(
            keyValueStore.getString(Constants.PREF_KEY_API_URL, Constants.DEFAULT_API_URL)
                ?: Constants.DEFAULT_API_URL,
            factory.isDebug(),
        )
    }

    private lateinit var factory: Factory
}