package com.ramitsuri.notificationjournal.core.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.navigation.NavBackStackEntry
import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.BuildKonfig
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.data.WearDataSharingClient
import com.ramitsuri.notificationjournal.core.data.dictionary.DictionaryDao
import com.ramitsuri.notificationjournal.core.log.InMemoryLogWriter
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import com.ramitsuri.notificationjournal.core.network.DataReceiveHelper
import com.ramitsuri.notificationjournal.core.network.DataReceiveHelperImpl
import com.ramitsuri.notificationjournal.core.network.DataSendHelper
import com.ramitsuri.notificationjournal.core.network.DataSendHelperImpl
import com.ramitsuri.notificationjournal.core.repository.ImportRepository
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.spellcheck.SpellChecker
import com.ramitsuri.notificationjournal.core.ui.addjournal.AddJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.editjournal.EditJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.DataStoreKeyValueStore
import com.ramitsuri.notificationjournal.core.utils.KeyValueStore
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelInfo
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelType
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import com.ramitsuri.notificationjournal.core.utils.PrefsKeyValueStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okio.Path.Companion.toOkioPath
import java.util.UUID

object ServiceLocator {
    fun init(factory: DiFactory) {
        ServiceLocator.factory = factory
        notificationHandler.init(
            listOf(
                NotificationChannelInfo(
                    channelType = NotificationChannelType.MAIN,
                    name = NotificationChannelType.MAIN.id,
                    description = "For main notification",
                ),
            ),
        )
        val deviceId = keyValueStore.getString(Constants.PREF_KEY_DEVICE_ID, "")
        if (deviceId.isNullOrEmpty()) {
            keyValueStore.putString(Constants.PREF_KEY_DEVICE_ID, UUID.randomUUID().toString())
        }
        Logger.setLogWriters(listOf(inMemoryLogWriter))
    }

    val allowJournalImport: Boolean
        get() = factory.allowJournalImport

    fun onAppStart() {
        startReceiving()
    }

    fun onAppStop() {
        coroutineScope.launch {
            launch {
                dataReceiveHelper?.closeConnection()
            }
            launch {
                dataSendHelper?.closeConnection()
            }
        }
    }

    fun resetReceiveHelper() {
        coroutineScope.launch {
            dataReceiveHelper?.closeConnection()
            dataReceiveHelper = null
            startReceiving()
        }
    }

    val repository: JournalRepository by lazy {
        JournalRepository(
            coroutineScope = coroutineScope,
            dao = AppDatabase.getInstance(factory::getDatabaseBuilder).journalEntryDao(),
            dataSendHelper = dataSendHelper,
            conflictDao = AppDatabase.getInstance(factory::getDatabaseBuilder).entryConflictDao(),
        )
    }

    val notificationHandler: NotificationHandler by lazy {
        factory.getNotificationHandler()
    }

    val keyValueStore: KeyValueStore by lazy {
        PrefsKeyValueStore(factory)
    }

    val tagsDao: TagsDao by lazy {
        AppDatabase.getInstance(factory::getDatabaseBuilder).tagsDao()
    }

    val templatesDao: JournalEntryTemplateDao by lazy {
        AppDatabase.getInstance(factory::getDatabaseBuilder).templateDao()
    }

    private val dictionaryDao: DictionaryDao by lazy {
        AppDatabase.getInstance(factory::getDatabaseBuilder).dictionaryDao()
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

    fun getAddJournalEntryVMFactory(navBackStackEntry: NavBackStackEntry) =
        factory.addJournalEntryVMFactory(
            navBackStackEntry,
        ) { savedStateHandle ->
            AddJournalEntryViewModel(
                savedStateHandle = savedStateHandle,
                repository = repository,
                tagsDao = tagsDao,
                templatesDao = templatesDao,
                spellChecker = spellChecker,
            )
        }

    fun getEditJournalEntryVMFactory(navBackStackEntry: NavBackStackEntry) =
        factory.editJournalEntryVMFactory(
            navBackStackEntry,
        ) { savedStateHandle ->
            EditJournalEntryViewModel(
                savedStateHandle = savedStateHandle,
                repository = repository,
                tagsDao = tagsDao,
                templatesDao = templatesDao,
                spellChecker = spellChecker,
            )
        }

    fun getAppVersion(): String {
        val suffix = if (BuildKonfig.IS_DEBUG) "_debug" else ""
        return "${BuildKonfig.APP_VERSION}$suffix"
    }

    val prefManager by lazy {
        val keyValueStore =
            DataStoreKeyValueStore(
                dataStore =
                    PreferenceDataStoreFactory.createWithPath(
                        produceFile = { factory.getDataStorePath().toOkioPath() },
                    ),
            )
        PrefManager(keyValueStore)
    }

    val dataSendHelper: DataSendHelper? by lazy {
        val hostName = keyValueStore.getString(Constants.PREF_KEY_DATA_HOST, "")
        val exchangeName = keyValueStore.getString(Constants.PREF_KEY_EXCHANGE_NAME, "")
        val deviceName = keyValueStore.getString(Constants.PREF_KEY_DEVICE_NAME, "")
        val deviceId = keyValueStore.getString(Constants.PREF_KEY_DEVICE_ID, "")
        val username = keyValueStore.getString(Constants.PREF_KEY_USERNAME, "")
        val password = keyValueStore.getString(Constants.PREF_KEY_PASSWORD, "")
        if (hostName.isNullOrEmpty() ||
            exchangeName.isNullOrEmpty() ||
            deviceName.isNullOrEmpty() ||
            deviceId.isNullOrEmpty() ||
            username.isNullOrEmpty() ||
            password.isNullOrEmpty()
        ) {
            null
        } else {
            DataSendHelperImpl(
                ioDispatcher = ioDispatcher,
                hostName = hostName,
                exchangeName = exchangeName,
                deviceName = deviceName,
                deviceId = deviceId,
                username = username,
                password = password,
                json = json,
            )
        }
    }

    private val spellChecker by lazy {
        SpellChecker(
            initializationScope = coroutineScope,
            ioDispatcher = ioDispatcher,
            defaultDispatcher = defaultDispatcher,
            dictionaryDao = dictionaryDao,
        )
    }

    private var dataReceiveHelper: DataReceiveHelper? = null
        get() =
            if (field == null) {
                synchronized(this) {
                    return if (field == null) getReceiver() else field
                }
            } else {
                field
            }

    val coroutineScope by lazy {
        CoroutineScope(SupervisorJob())
    }

    val inMemoryLogWriter by lazy {
        InMemoryLogWriter()
    }

    val importRepository: ImportRepository
        get() = factory.getImportRepository(ioDispatcher)

    private val ioDispatcher by lazy {
        Dispatchers.IO
    }

    private val defaultDispatcher by lazy {
        Dispatchers.Default
    }

    private fun getReceiver(): DataReceiveHelper? {
        val hostName = keyValueStore.getString(Constants.PREF_KEY_DATA_HOST, "")
        val exchangeName = keyValueStore.getString(Constants.PREF_KEY_EXCHANGE_NAME, "")
        val deviceName = keyValueStore.getString(Constants.PREF_KEY_DEVICE_NAME, "")
        val deviceId = keyValueStore.getString(Constants.PREF_KEY_DEVICE_ID, "")
        val username = keyValueStore.getString(Constants.PREF_KEY_USERNAME, "")
        val password = keyValueStore.getString(Constants.PREF_KEY_PASSWORD, "")
        return if (hostName.isNullOrEmpty() ||
            exchangeName.isNullOrEmpty() ||
            deviceName.isNullOrEmpty() ||
            deviceId.isNullOrEmpty() ||
            username.isNullOrEmpty() ||
            password.isNullOrEmpty()
        ) {
            null
        } else {
            DataReceiveHelperImpl(
                ioDispatcher = ioDispatcher,
                hostName = hostName,
                exchangeName = exchangeName,
                deviceName = deviceName,
                deviceId = deviceId,
                username = username,
                password = password,
                json = json,
            )
        }
    }

    private fun startReceiving() {
        coroutineScope.launch {
            dataReceiveHelper?.startListening {
                when (it) {
                    is Payload.Entries -> {
                        coroutineScope.launch { repository.handlePayload(it) }
                    }

                    is Payload.Tags -> {
                        coroutineScope.launch { tagsDao.clearAndInsert(it.data) }
                    }

                    is Payload.Templates -> {
                        coroutineScope.launch { templatesDao.clearAndInsert(it.data) }
                    }

                    is Payload.ClearDaysAndInsertEntries -> {
                        coroutineScope.launch {
                            repository.clearDaysAndInsert(
                                days = it.days,
                                entries = it.entries,
                                // We're receiving entries here, we don't want to send them back
                                uploadEntries = false,
                            )
                        }
                    }
                }
            }
        }
    }

    private lateinit var factory: DiFactory
}
