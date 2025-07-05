package com.ramitsuri.notificationjournal.core.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import com.ramitsuri.notificationjournal.core.BuildKonfig
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.EntryConflictDao
import com.ramitsuri.notificationjournal.core.data.JournalEntryDao
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.data.WearDataSharingClient
import com.ramitsuri.notificationjournal.core.data.dictionary.DictionaryDao
import com.ramitsuri.notificationjournal.core.log.InMemoryLogWriter
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.model.WindowPosition
import com.ramitsuri.notificationjournal.core.model.WindowSize
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Entity
import com.ramitsuri.notificationjournal.core.network.DataReceiveHelperImpl
import com.ramitsuri.notificationjournal.core.network.DataSendHelper
import com.ramitsuri.notificationjournal.core.network.DataSendHelperImpl
import com.ramitsuri.notificationjournal.core.network.VerifyEntriesHelper
import com.ramitsuri.notificationjournal.core.repository.ExportRepository
import com.ramitsuri.notificationjournal.core.repository.ImportRepository
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.spellcheck.SpellChecker
import com.ramitsuri.notificationjournal.core.ui.addjournal.AddJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.editjournal.EditJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.journalentryday.ViewJournalEntryDayViewModel
import com.ramitsuri.notificationjournal.core.utils.DataStoreKeyValueStore
import com.ramitsuri.notificationjournal.core.utils.Importance
import com.ramitsuri.notificationjournal.core.utils.JournalEntryNotificationHelper
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelInfo
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelType
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import com.ramitsuri.notificationjournal.core.utils.PrefsKeyValueStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okio.Path.Companion.toOkioPath
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

object ServiceLocator {
    private lateinit var db: AppDatabase
    private var notificationHelper: JournalEntryNotificationHelper? = null
    private var receiveJob: Job? = null
    private var appStopJob: Job? = null

    fun init(
        factory: DiFactory,
        notificationHelper: JournalEntryNotificationHelper? = null,
    ) {
        ServiceLocator.factory = factory
        ServiceLocator.notificationHelper = notificationHelper
        db = AppDatabase.getDatabase { factory.getDatabaseBuilder() }
        notificationHandler.init(
            listOf(
                NotificationChannelInfo(
                    channelType = NotificationChannelType.MAIN,
                    name = NotificationChannelType.MAIN.id,
                    description = "For main notification",
                ),
                NotificationChannelInfo(
                    channelType = NotificationChannelType.REMINDERS,
                    name = NotificationChannelType.REMINDERS.id,
                    description = "For reminder notification",
                    importance = Importance.HIGH,
                    playSound = true,
                    vibrate = true,
                    showBadge = true,
                ),
            ),
        )
        runBlocking {
            migrateFromKeyValueStore()
            prefManager.getDataHostProperties().first().let { dataHostProperties ->
                if (dataHostProperties.deviceId.isEmpty()) {
                    prefManager.setDataHostProperties(dataHostProperties.copy(deviceId = UUID.randomUUID().toString()))
                }
            }
        }
        Logger.setLogWriters(inMemoryLogWriter, platformLogWriter())
    }

    val allowJournalImport: Boolean
        get() = factory.allowJournalImport

    fun onAppStart() {
        appStopJob?.cancel()
        startReceiving()
        verifyEntriesHelper.start()
    }

    fun onAppStop() {
        appStopJob =
            coroutineScope.launch {
                Logger.i(TAG) { "Will wait for 2 minutes before stopping jobs" }
                delay(2.minutes)
                Logger.i(TAG) { "Stopping jobs" }
                launch {
                    receiveJob?.cancel()
                }
                verifyEntriesHelper.stop()
            }
    }

    fun resetReceiveHelper() {
        coroutineScope.launch {
            startReceiving()
        }
    }

    val verifyEntriesHelper: VerifyEntriesHelper by lazy {
        VerifyEntriesHelper(
            coroutineScope = coroutineScope,
            ioDispatcher = ioDispatcher,
            dataSendHelper = dataSendHelper,
            dataReceiveHelper = dataReceiveHelper,
            repository = repository,
        )
    }

    val repository: JournalRepository by lazy {
        JournalRepository(
            dao = db.journalEntryDao(),
            dataSendHelper = dataSendHelper,
            conflictDao = db.entryConflictDao(),
            prefManager = prefManager,
        )
    }

    val notificationHandler: NotificationHandler by lazy {
        factory.getNotificationHandler()
    }

    val journalEntryDao: JournalEntryDao?
        get() = if (BuildKonfig.IS_DEBUG) db.journalEntryDao() else null

    val conflictDao: EntryConflictDao?
        get() = if (BuildKonfig.IS_DEBUG) db.entryConflictDao() else null

    val tagsDao: TagsDao
        get() = db.tagsDao()

    val templatesDao: JournalEntryTemplateDao
        get() = db.templateDao()

    private val dictionaryDao: DictionaryDao
        get() = db.dictionaryDao()

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
                prefManager = prefManager,
            )
        }

    fun getViewJournalEntryDayVMFactory(navBackStackEntry: NavBackStackEntry) =
        factory.viewJournalEntryDayVMFactory(
            navBackStackEntry,
        ) { savedStateHandle ->
            ViewJournalEntryDayViewModel(
                savedStateHandle = savedStateHandle,
                repository = repository,
                tagsDao = tagsDao,
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
                prefManager = prefManager,
            )
        }

    fun getAppVersion(): String {
        val suffix = if (BuildKonfig.IS_DEBUG) "_debug" else ""
        return "${BuildKonfig.APP_VERSION}$suffix"
    }

    fun showNotification(
        journalEntry: JournalEntry,
        inTime: Duration,
    ) {
        notificationHelper?.show(journalEntry, inTime)
    }

    fun allowJournalEntryNotify(): Boolean {
        return notificationHelper != null
    }

    fun getJournalEntryScreenDeepLinks(): List<NavDeepLink> = factory.getJournalEntryScreenDeepLinks()

    fun getAddEntryScreenDeepLinks(): List<NavDeepLink> = factory.getAddEntryScreenDeepLinks()

    val prefManager by lazy {
        val keyValueStore =
            DataStoreKeyValueStore(
                dataStore =
                    PreferenceDataStoreFactory.createWithPath(
                        produceFile = { factory.getDataStorePath().toOkioPath() },
                    ),
            )
        PrefManager(keyValueStore = keyValueStore, json = json)
            .also {
                coroutineScope.launch {
                    it.removeLegacy(
                        booleanPrefs =
                            listOf(
                                "show_logs_button",
                                "show_reconciled",
                            ),
                    )
                }
            }
    }

    val dataSendHelper: DataSendHelper by lazy {
        DataSendHelperImpl(
            getDataHostProperties = prefManager.getDataHostProperties()::first,
        )
    }

    private val spellChecker by lazy {
        SpellChecker(
            initializationScope = coroutineScope,
            ioDispatcher = ioDispatcher,
            defaultDispatcher = defaultDispatcher,
            dictionaryDao = dictionaryDao,
        )
    }

    private val dataReceiveHelper by lazy {
        DataReceiveHelperImpl(
            coroutineScope = coroutineScope,
            ioDispatcher = ioDispatcher,
            getDataHostProperties = prefManager.getDataHostProperties()::first,
        )
    }

    val coroutineScope by lazy {
        CoroutineScope(SupervisorJob())
    }

    val inMemoryLogWriter by lazy {
        InMemoryLogWriter()
    }

    val importRepository: ImportRepository
        get() = factory.getImportRepository(ioDispatcher)

    val exportRepository: ExportRepository?
        get() = factory.getExportRepository(ioDispatcher)

    private val ioDispatcher by lazy {
        Dispatchers.IO
    }

    private val defaultDispatcher by lazy {
        Dispatchers.Default
    }

    private fun startReceiving() {
        Logger.i(TAG) { "Start receiving" }
        receiveJob?.cancel()
        receiveJob =
            coroutineScope.launch {
                dataReceiveHelper.reset()
                dataReceiveHelper.payloadFlow.filterIsInstance<Entity>().collect {
                    when (it) {
                        is Entity.Entries -> {
                            coroutineScope.launch { repository.handlePayload(it) }
                        }

                        is Entity.Tags -> {
                            coroutineScope.launch { tagsDao.clearAndInsert(it.data) }
                        }

                        is Entity.Templates -> {
                            coroutineScope.launch { templatesDao.clearAndInsert(it.data) }
                        }

                        is Entity.ClearDaysAndInsertEntries -> {
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

    private suspend fun migrateFromKeyValueStore() {
        val oldKVStore = PrefsKeyValueStore(factory)
        if (!oldKVStore.hasKeys()) {
            return
        }
        DataHostProperties(
            deviceName = oldKVStore.getString("device_name") ?: "",
            deviceId = oldKVStore.getString("device_id") ?: "",
            exchangeName = oldKVStore.getString("exchange_name") ?: "",
            dataHost = oldKVStore.getString("data_host") ?: "",
            username = oldKVStore.getString("username") ?: "",
            password = oldKVStore.getString("password") ?: "",
        ).let {
            prefManager.setDataHostProperties(it)
        }

        WindowSize(
            height = oldKVStore.getInt("window_size_height", 0).toFloat(),
            width = oldKVStore.getInt("window_size_width", 0).toFloat(),
        ).let {
            prefManager.setWindowSize(it)
        }

        WindowPosition(
            x = oldKVStore.getInt("window_position_x", 0).toFloat(),
            y = oldKVStore.getInt("window_position_y", 0).toFloat(),
        ).let {
            prefManager.setWindowPosition(it)
        }
        oldKVStore.clear()
    }

    private lateinit var factory: DiFactory
    private const val TAG = "ServiceLocator"
}
