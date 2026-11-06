package com.ramitsuri.notificationjournal.core.di

import androidx.room.RoomDatabase
import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.EntryConflictDao
import com.ramitsuri.notificationjournal.core.data.JournalEntryDao
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.data.VoiceNoteDao
import com.ramitsuri.notificationjournal.core.data.dictionary.DictionaryDao
import com.ramitsuri.notificationjournal.core.log.InMemoryLogWriter
import com.ramitsuri.notificationjournal.core.network.DataReceiveHelper
import com.ramitsuri.notificationjournal.core.network.DataReceiveHelperImpl
import com.ramitsuri.notificationjournal.core.network.DataSendHelper
import com.ramitsuri.notificationjournal.core.network.DataSendHelperImpl
import com.ramitsuri.notificationjournal.core.network.VerifyEntriesHelper
import com.ramitsuri.notificationjournal.core.network.WebSocketHelper
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.spellcheck.SpellChecker
import com.ramitsuri.notificationjournal.core.utils.EncryptionHelper
import com.ramitsuri.notificationjournal.core.utils.KeyValueStoreV2
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@ContributesTo(AppScope::class)
@BindingContainer
object CoreBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideAppDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
        AppDatabase.getDatabase { builder }

    @Provides
    fun provideJournalEntryDao(db: AppDatabase): JournalEntryDao = db.journalEntryDao()

    @Provides
    fun provideEntryConflictDao(db: AppDatabase): EntryConflictDao = db.entryConflictDao()

    @Provides
    fun provideTagsDao(db: AppDatabase): TagsDao = db.tagsDao()

    @Provides
    fun provideTemplatesDao(db: AppDatabase): JournalEntryTemplateDao = db.templateDao()

    @Provides
    fun provideVoiceNoteDao(db: AppDatabase): VoiceNoteDao = db.voiceNoteDao()

    @Provides
    fun provideDictionaryDao(db: AppDatabase): DictionaryDao = db.dictionaryDao()

    @Provides
    @SingleIn(AppScope::class)
    fun provideJson(): Json =
        Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        }

    @Provides
    @SingleIn(AppScope::class)
    fun provideCoroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob())

    @Provides
    fun provideClock(): kotlin.time.Clock = kotlin.time.Clock.System

    @Provides
    fun provideTimeZone(): kotlinx.datetime.TimeZone = kotlinx.datetime.TimeZone.currentSystemDefault()

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @SingleIn(AppScope::class)
    fun provideEncryptionHelper(keyValueStore: KeyValueStoreV2): EncryptionHelper = EncryptionHelper(keyValueStore)

    @Provides
    @SingleIn(AppScope::class)
    fun providePrefManager(
        keyValueStore: KeyValueStoreV2,
        json: Json,
        scope: CoroutineScope,
    ): PrefManager =
        PrefManager(json = json, keyValueStore = keyValueStore).also {
            scope.launch {
                it.removeLegacy(listOf("show_logs_button", "show_reconciled", "show_suggestions"))
            }
        }

    @Provides
    @SingleIn(AppScope::class)
    fun provideWebSocketHelper(
        json: Json,
        encryptionHelper: EncryptionHelper,
    ): WebSocketHelper =
        WebSocketHelper(
            json = json,
            httpClient =
                HttpClient {
                    install(io.ktor.client.plugins.websocket.WebSockets)
                },
            encryptionHelper = encryptionHelper,
        )

    @Provides
    @SingleIn(AppScope::class)
    fun provideDataSendHelper(
        prefManager: PrefManager,
        webSocketHelper: WebSocketHelper,
    ): DataSendHelper =
        DataSendHelperImpl(
            getDataHostProperties = prefManager.getDataHostProperties()::first,
            webSocketHelper = webSocketHelper,
        )

    @Provides
    @SingleIn(AppScope::class)
    fun provideDataReceiveHelper(webSocketHelper: WebSocketHelper): DataReceiveHelper =
        DataReceiveHelperImpl(webSocketHelper)

    @Provides
    @SingleIn(AppScope::class)
    fun provideVerifyEntriesHelper(
        scope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        dataSendHelper: DataSendHelper,
        dataReceiveHelper: DataReceiveHelper,
        repository: JournalRepository,
    ): VerifyEntriesHelper =
        VerifyEntriesHelper(
            coroutineScope = scope,
            ioDispatcher = ioDispatcher,
            dataSendHelper = dataSendHelper,
            dataReceiveHelper = dataReceiveHelper,
            repository = repository,
        )

    @Provides
    @SingleIn(AppScope::class)
    fun provideSpellChecker(
        scope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
        dictionaryDao: DictionaryDao,
    ): SpellChecker =
        SpellChecker(
            initializationScope = scope,
            ioDispatcher = ioDispatcher,
            defaultDispatcher = defaultDispatcher,
            dictionaryDao = dictionaryDao,
        )

    @Provides
    @SingleIn(AppScope::class)
    fun provideInMemoryLogWriter(): InMemoryLogWriter =
        InMemoryLogWriter().also {
            Logger.setLogWriters(it, platformLogWriter())
        }
}
