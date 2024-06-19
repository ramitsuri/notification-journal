package com.ramitsuri.notificationjournal.core.di

import androidx.navigation.NavBackStackEntry
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.JournalEntryDao
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.data.WearDataSharingClient
import com.ramitsuri.notificationjournal.core.network.DataReceiveHelper
import com.ramitsuri.notificationjournal.core.network.DataReceiveHelperImpl
import com.ramitsuri.notificationjournal.core.network.DataSendHelper
import com.ramitsuri.notificationjournal.core.network.DataSendHelperImpl
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.ui.addjournal.AddJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.editjournal.EditJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.KeyValueStore
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelInfo
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelType
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.ramitsuri.notificationjournal.core.utils.PrefsKeyValueStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

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
        val deviceId = keyValueStore.getString(Constants.PREF_KEY_DEVICE_ID, "")
        if (deviceId.isNullOrEmpty()) {
            keyValueStore.putString(Constants.PREF_KEY_DEVICE_ID, UUID.randomUUID().toString())
        }
    }

    fun onAppStart() {
        coroutineScope.launch {
            dataReceiveHelper?.startListening {

            }
        }
    }

    fun onAppStop() {
        coroutineScope.launch {
            dataReceiveHelper?.closeConnection()
        }
    }

    val repository: JournalRepository by lazy {
        JournalRepository(
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

    private val json by lazy {
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

    val dataSendHelper: DataSendHelper? by lazy {
        val hostName = keyValueStore.getString(Constants.PREF_KEY_DATA_HOST, "")
        val exchangeName = keyValueStore.getString(Constants.PREF_KEY_EXCHANGE_NAME, "")
        val deviceName = keyValueStore.getString(Constants.PREF_KEY_DEVICE_NAME, "")
        val deviceId = keyValueStore.getString(Constants.PREF_KEY_DEVICE_ID, "")
        if (hostName.isNullOrEmpty() ||
            exchangeName.isNullOrEmpty() ||
            deviceName.isNullOrEmpty() ||
            deviceId.isNullOrEmpty()
        ) {
            null
        } else {
            DataSendHelperImpl(
                ioDispatcher = ioDispatcher,
                hostName = hostName,
                exchangeName = exchangeName,
                deviceName = deviceName,
                deviceId = deviceId,
                json = json,
            )
        }
    }

    private val dataReceiveHelper: DataReceiveHelper? by lazy {
        val hostName = keyValueStore.getString(Constants.PREF_KEY_DATA_HOST, "")
        val exchangeName = keyValueStore.getString(Constants.PREF_KEY_EXCHANGE_NAME, "")
        val deviceName = keyValueStore.getString(Constants.PREF_KEY_DEVICE_NAME, "")
        val deviceId = keyValueStore.getString(Constants.PREF_KEY_DEVICE_ID, "")
        if (hostName.isNullOrEmpty() ||
            exchangeName.isNullOrEmpty() ||
            deviceName.isNullOrEmpty() ||
            deviceId.isNullOrEmpty()
        ) {
            null
        } else {
            DataReceiveHelperImpl(
                ioDispatcher = ioDispatcher,
                hostName = hostName,
                exchangeName = exchangeName,
                deviceName = deviceName,
                deviceId = deviceId,
                json = json,
            )
        }
    }

    private val coroutineScope by lazy {
        CoroutineScope(SupervisorJob())
    }

    private val ioDispatcher by lazy {
        Dispatchers.IO
    }

    private lateinit var factory: Factory
}