package com.ramitsuri.notificationjournal.core.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ramitsuri.notificationjournal.core.BuildKonfig
import com.ramitsuri.notificationjournal.core.ExportRepositoryImpl
import com.ramitsuri.notificationjournal.core.ImportRepositoryImpl
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.WearDataSharingClient
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.repository.ExportRepository
import com.ramitsuri.notificationjournal.core.repository.ImportRepository
import com.ramitsuri.notificationjournal.core.utils.DataStoreKeyValueStore
import com.ramitsuri.notificationjournal.core.utils.JournalEntryNotificationHelper
import com.ramitsuri.notificationjournal.core.utils.KeyValueStoreV2
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelInfo
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.ramitsuri.notificationjournal.core.utils.NotificationInfo
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.datetime.LocalDateTime
import okio.Path.Companion.toOkioPath
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@ContributesTo(AppScope::class)
@BindingContainer
object DesktopBindings {
    private val appDir: Path by lazy {
        System
            .getProperty("user.home")
            .let { userHomePath ->
                val osPath =
                    System
                        .getProperty("os.name", "generic")
                        .lowercase()
                        .let { os ->
                            if ((os.indexOf("mac") >= 0) || (os.indexOf("darwin") >= 0)) {
                                "Library"
                            } else if (os.indexOf("win") >= 0) {
                                "Documents"
                            } else if (os.indexOf("linux") >= 0) {
                                "Documents"
                            } else {
                                "Journal"
                            }
                        }
                val appPath = "com.ramitsuri.notificationjournal"
                val buildPath = if (BuildKonfig.IS_DEBUG) "debug" else "release"
                Paths.get(userHomePath, osPath, appPath, buildPath)
            }
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
        val testDir = System.getProperty("test.datastore.dir")
        val dir = if (testDir != null) Paths.get(testDir) else appDir
        Files.createDirectories(dir)
        val fileName = "database"
        val dbFile = dir.resolve(fileName).toFile()
        return Room.databaseBuilder<AppDatabase>(
            name = dbFile.absolutePath,
        )
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideKeyValueStore(): KeyValueStoreV2 {
        val testDir = System.getProperty("test.datastore.dir")
        val dir = if (testDir != null) Paths.get(testDir) else appDir
        Files.createDirectories(dir)
        val path = dir.resolve(DataStoreKeyValueStore.FILE)
        return DataStoreKeyValueStore(
            dataStore =
                PreferenceDataStoreFactory.createWithPath(
                    produceFile = { path.toOkioPath() },
                ),
        )
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideImportRepository(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ImportRepository = ImportRepositoryImpl(ioDispatcher)

    @Provides
    @SingleIn(AppScope::class)
    fun provideExportRepository(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ExportRepository = ExportRepositoryImpl(ioDispatcher)

    @Provides
    fun provideOptionalExportRepository(exportRepository: ExportRepository): ExportRepository? = exportRepository

    @Provides
    @SingleIn(AppScope::class)
    fun provideWearDataSharingClient(): WearDataSharingClient =
        object : WearDataSharingClient {
            override suspend fun postJournalEntry(
                value: String,
                time: LocalDateTime,
                tag: String?,
            ): Boolean = true

            override suspend fun requestUpload() {}

            override suspend fun postTemplate(template: JournalEntryTemplate): Boolean = true

            override suspend fun clearTemplates(): Boolean = true

            override suspend fun updateTile(): Boolean = true
        }

    @Provides
    @SingleIn(AppScope::class)
    fun provideNotificationHandler(): NotificationHandler =
        object : NotificationHandler {
            override fun init(channels: List<NotificationChannelInfo>) {}

            override fun showNotification(notificationInfo: NotificationInfo) {}

            override fun getNotificationId(notificationInfo: NotificationInfo): Int = 1
        }

    @Provides
    @SingleIn(AppScope::class)
    fun providePlatformConfig(): PlatformConfig =
        PlatformConfig(
            isDesktop = true,
            allowJournalImport = true,
            actsAsServer = true,
            allowJournalEntryNotify = false,
            ipAddress = getIpAddress(),
        )

    @Provides
    fun provideJournalEntryNotificationHelper(): JournalEntryNotificationHelper? = null

    private fun getIpAddress(): String? =
        try {
            java.net.DatagramSocket().use { socket ->
                socket.connect(java.net.InetAddress.getByName("8.8.8.8"), 10002)
                socket.localAddress.hostAddress
            }
        } catch (e: Exception) {
            null
        }
}
