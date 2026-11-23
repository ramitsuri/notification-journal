package com.ramitsuri.notificationjournal.core.work

import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.model.WorkProfile
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Entity
import com.ramitsuri.notificationjournal.core.model.sync.Sender
import com.ramitsuri.notificationjournal.core.network.WebSocketHelper
import com.ramitsuri.notificationjournal.core.ui.settings.SettingsViewModel
import com.ramitsuri.notificationjournal.core.utils.BaseTest
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.EncryptionHelper
import com.ramitsuri.notificationjournal.core.utils.Key
import com.ramitsuri.notificationjournal.core.utils.QrCodeParser
import com.ramitsuri.notificationjournal.core.utils.WorkProfileManager
import com.ramitsuri.notificationjournal.core.utils.getTestDataKeyValueStore
import com.ramitsuri.notificationjournal.core.utils.testJson
import com.ramitsuri.notificationjournal.server.module
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.server.application.Application
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WorkQuickCaptureEndToEndTest : BaseTest() {
    private var port: Int = 0
    private var server: EmbeddedServer<*, *>? = null
    private lateinit var httpClient: HttpClient
    private lateinit var workKeyValueStore: com.ramitsuri.notificationjournal.core.utils.KeyValueStoreV2
    private lateinit var workProfileManager: WorkProfileManager
    private lateinit var settingsViewModel: SettingsViewModel

    private val testSalt = Base64.encode(ByteArray(16) { 0x42 })
    private val testPassword = "mac-work-session-password-xyz"

    @Before
    fun setup() {
        port = ServerSocket(0).use { it.localPort }
        server =
            embeddedServer(Netty, port = port, module = Application::module)
                .start(wait = false)

        httpClient =
            HttpClient {
                install(WebSockets)
            }

        workKeyValueStore = getTestDataKeyValueStore()
        workProfileManager = WorkProfileManager(workKeyValueStore)

        settingsViewModel =
            SettingsViewModel(
                repository = repository,
                prefManager = prefManager,
                getAppVersion = { "1.0" },
                journalEntryDao = db.journalEntryDao(),
                conflictDao = db.entryConflictDao(),
                enableExport = true,
                encryptionHelper = encryptionHelper,
                getIpAddress = { null },
                actsAsServer = false,
            )
    }

    @After
    fun tearDown() {
        server?.stop(100, 200)
        server = null
        httpClient.close()
    }

    @Test
    fun testPairingQrCodeParsingAndCrossScanProtection() =
        runBlocking {
            // Seed personal home sync credentials and host
            prefManager.setDataHostProperties(DataHostProperties(dataHost = "http://my-home-desktop:8000"))
            encryptionHelper.setSalt("home-salt-value")
            encryptionHelper.setPassword("home-password-value")

            // macOS WorkAppState generates pairing string with work namespace
            val macQrString = "work;;;$testSalt;;;$testPassword;;;192.168.1.150:8080"

            // Verify work QR parser accepts and decomposes it
            assertTrue(QrCodeParser.isWorkQr(macQrString))
            val parsedWorkProfile = QrCodeParser.parseWorkQr(macQrString)
            assertNotNull(parsedWorkProfile)
            assertEquals("192.168.1.150", parsedWorkProfile!!.host)
            assertEquals(8080, parsedWorkProfile.port)
            assertEquals(testSalt, parsedWorkProfile.salt)
            assertEquals(testPassword, parsedWorkProfile.password)

            workProfileManager.setProfile(parsedWorkProfile)
            val stored = workProfileManager.getProfile()
            assertEquals("192.168.1.150", stored?.host)
            assertEquals(8080, stored?.port)

            // Cross-scan protection: home scanner rejects the work QR code
            assertNull(QrCodeParser.parseHomeQr(macQrString))
            settingsViewModel.onQrCodeScanned(macQrString)

            // Home settings must remain intact with zero contamination
            assertEquals("http://my-home-desktop:8000", prefManager.getDataHostProperties().first().dataHost)
            assertEquals("home-salt-value", encryptionHelper.getSalt().first())
            assertEquals("home-password-value", encryptionHelper.getPassword().first())
        }

    @Test
    fun testFullLoopIngestionAckAndSubsequentHomeDesktopSync() =
        runBlocking {
            workProfileManager.setProfile(
                WorkProfile(
                    host = "localhost",
                    port = port,
                    salt = testSalt,
                    password = testPassword,
                ),
            )

            val serverEncryptionHelper =
                EncryptionHelper(
                    keyValueStore = workKeyValueStore,
                    passwordKey = Key.WORK_PASSWORD,
                    saltKey = Key.WORK_SALT,
                )
            val serverSocketHelper =
                WebSocketHelper(
                    httpClient = httpClient,
                    json = testJson,
                    encryptionHelper = serverEncryptionHelper,
                    port = port,
                )

            val receivedEntitiesByServer = mutableListOf<Entity>()
            serverSocketHelper.setEntityHandler { entity ->
                receivedEntitiesByServer.add(entity)
            }

            val serverJob =
                launch(Dispatchers.IO) {
                    serverSocketHelper.start(host = "localhost", port = port)
                }
            serverSocketHelper.isConnected.first { it }

            val controller =
                WorkBridgeController(
                    repository = repository,
                    workProfileManager = workProfileManager,
                    keyValueStore = workKeyValueStore,
                    json = testJson,
                    httpClient = httpClient,
                    retryDelay = 50.milliseconds,
                    maxRetries = 3,
                )

            val controllerJob = launch(Dispatchers.IO) { controller.start() }
            controller.state.first { it is WorkBridgeController.State.Connected }

            // 1. Work Mac streams encrypted entry
            val testEntry =
                JournalEntry(
                    id = "work-e2e-entry-1",
                    entryTime = LocalDateTime.parse("2026-11-22T14:30:00"),
                    text = "End to end note captured at work",
                    tag = "internal_no_tag_value",
                    uploaded = false,
                    replacesLocal = false,
                )
            val workPayload =
                Entity.Entries(
                    sender = Sender(name = "Work Mac", id = "work-mac"),
                    data = listOf(testEntry),
                )

            val ackReceived = serverSocketHelper.send(workPayload, timeout = 5.seconds)
            assertTrue("Server must receive ACK from Android for work payload", ackReceived)

            // 2. Android receives, decrypts, and persists with uploaded = false
            val persisted = repository.get("work-e2e-entry-1")
            assertNotNull(persisted)
            assertEquals("End to end note captured at work", persisted?.text)
            assertFalse("Work entries must be persisted with uploaded = false", persisted?.uploaded ?: true)
            assertFalse("Work entries must be persisted with replacesLocal = false", persisted?.replacesLocal ?: true)

            // 3. Zero data leakage: phone never queries or pushes past entries to Mac
            kotlinx.coroutines.delay(200)
            assertTrue(
                "Phone must never send past entries or sync requests over the work bridge",
                receivedEntitiesByServer.isEmpty(),
            )

            // 4. Subsequent Home Desktop Sync: UploadWorker (repository.uploadAll) pushes to personal desktop
            val forUpload = repository.getForUpload()
            assertEquals(1, forUpload.size)
            assertEquals("work-e2e-entry-1", forUpload.first().id)

            dataSendHelper.sendSuccessful = true
            repository.uploadAll()

            assertEquals(listOf("work-e2e-entry-1"), dataSendHelper.entriesSent.map { it.id })
            val synced = repository.get("work-e2e-entry-1")
            assertTrue("Entry must be marked uploaded = true after Home Desktop sync", synced?.uploaded ?: false)

            controllerJob.cancel()
            serverSocketHelper.stop()
            serverJob.cancel()
        }

    @Test
    fun testUsbModeLoopbackConnectionAndIngestion() =
        runBlocking {
            // USB mode uses 127.0.0.1 (forwarded via adb reverse tcp:<port> tcp:<port>)
            val usbQrString = "work;;;$testSalt;;;$testPassword;;;127.0.0.1:$port"
            val usbProfile = QrCodeParser.parseWorkQr(usbQrString)
            assertNotNull(usbProfile)
            assertEquals("127.0.0.1", usbProfile!!.host)
            assertEquals(port, usbProfile.port)

            workProfileManager.setProfile(usbProfile)

            val serverEncryptionHelper =
                EncryptionHelper(
                    keyValueStore = workKeyValueStore,
                    passwordKey = Key.WORK_PASSWORD,
                    saltKey = Key.WORK_SALT,
                )
            val serverSocketHelper =
                WebSocketHelper(
                    httpClient = httpClient,
                    json = testJson,
                    encryptionHelper = serverEncryptionHelper,
                    port = port,
                )

            val serverJob =
                launch(Dispatchers.IO) {
                    serverSocketHelper.start(host = "127.0.0.1", port = port)
                }
            serverSocketHelper.isConnected.first { it }

            val controller =
                WorkBridgeController(
                    repository = repository,
                    workProfileManager = workProfileManager,
                    keyValueStore = workKeyValueStore,
                    json = testJson,
                    httpClient = httpClient,
                    retryDelay = 50.milliseconds,
                    maxRetries = 3,
                )

            val controllerJob = launch(Dispatchers.IO) { controller.start() }
            controller.state.first { it is WorkBridgeController.State.Connected }

            val usbEntry =
                JournalEntry(
                    id = "usb-entry-1",
                    entryTime = LocalDateTime.parse("2026-11-22T16:00:00"),
                    text = "Captured over USB adb reverse",
                    tag = "internal_no_tag_value",
                    uploaded = false,
                    replacesLocal = false,
                )
            val ack = serverSocketHelper.send(Entity.Entries(listOf(usbEntry)), timeout = 5.seconds)
            assertTrue("USB connection must successfully deliver and receive ACK", ack)

            val persisted = repository.get("usb-entry-1")
            assertNotNull(persisted)
            assertEquals("Captured over USB adb reverse", persisted?.text)
            assertFalse(persisted?.uploaded ?: true)

            controllerJob.cancel()
            serverSocketHelper.stop()
            serverJob.cancel()
        }

    @Test
    fun testDisconnectTransitionsToConnectingAndAutoStops() =
        runBlocking {
            workProfileManager.setProfile(
                WorkProfile(
                    host = "localhost",
                    port = port,
                    salt = testSalt,
                    password = testPassword,
                ),
            )

            val stopRequestedDeferred = CompletableDeferred<Unit>()
            val controller =
                WorkBridgeController(
                    repository = repository,
                    workProfileManager = workProfileManager,
                    keyValueStore = workKeyValueStore,
                    json = testJson,
                    httpClient = httpClient,
                    retryDelay = 50.milliseconds,
                    maxRetries = 3,
                    onStopRequested = { stopRequestedDeferred.complete(Unit) },
                )

            val controllerJob = launch(Dispatchers.IO) { controller.start() }
            controller.state.first { it is WorkBridgeController.State.Connected }

            // Stop server to simulate connection drop
            server?.stop(50, 100)
            server = null

            // Controller retries 3 times and auto-stops
            stopRequestedDeferred.await()
            assertEquals(WorkBridgeController.State.Stopped, controller.state.value)

            controllerJob.cancel()
        }
}
