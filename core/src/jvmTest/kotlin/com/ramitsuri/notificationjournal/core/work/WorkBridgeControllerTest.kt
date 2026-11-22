package com.ramitsuri.notificationjournal.core.work

import com.ramitsuri.notificationjournal.core.model.WorkProfile
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Entity
import com.ramitsuri.notificationjournal.core.network.WebSocketHelper
import com.ramitsuri.notificationjournal.core.utils.BaseTest
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.EncryptionHelper
import com.ramitsuri.notificationjournal.core.utils.Key
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.nio.file.Paths
import kotlin.io.encoding.Base64
import kotlin.io.path.deleteRecursively
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WorkBridgeControllerTest : BaseTest() {
    private var port: Int = 0
    private var server: EmbeddedServer<*, *>? = null
    private lateinit var httpClient: HttpClient
    private lateinit var testWorkProfileManager: WorkProfileManager
    private lateinit var workKeyValueStore: com.ramitsuri.notificationjournal.core.utils.KeyValueStoreV2

    private val testSalt = Base64.encode(ByteArray(16) { 0x01 })
    private val testPassword = "test-work-password-123"

    @Before
    fun setupWorkBridge() {
        port = ServerSocket(0).use { it.localPort }
        server =
            embeddedServer(Netty, port = port, module = Application::module)
                .start(wait = false)

        httpClient =
            HttpClient {
                install(WebSockets)
            }

        workKeyValueStore = getTestDataKeyValueStore()
        testWorkProfileManager = WorkProfileManager(workKeyValueStore)
    }

    @After
    fun tearDownWorkBridge() {
        server?.stop(100, 200)
        server = null
        httpClient.close()
        Paths.get(Constants.BASE_DIR).deleteRecursively()
    }

    @Test
    fun testOneWayIngestionAndAck() =
        runBlocking {
            testWorkProfileManager.setProfile(
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

            val serverJob =
                launch(Dispatchers.IO) {
                    serverSocketHelper.start(host = "localhost", port = port)
                }
            serverSocketHelper.isConnected.first { it }

            val controller =
                WorkBridgeController(
                    repository = repository,
                    workProfileManager = testWorkProfileManager,
                    keyValueStore = workKeyValueStore,
                    json = testJson,
                    httpClient = httpClient,
                    retryDelay = 50.milliseconds,
                    maxRetries = 3,
                )

            val controllerJob = launch(Dispatchers.IO) { controller.start() }

            controller.state.first { it is WorkBridgeController.State.Connected }

            val testEntry =
                JournalEntry(
                    id = "work-entry-1",
                    entryTime = LocalDateTime.parse("2026-11-22T10:00:00"),
                    text = "Note captured from Work Mac",
                    tag = "internal_no_tag_value",
                    uploaded = true,
                    replacesLocal = false,
                )
            val outgoingPayload = Entity.Entries(listOf(testEntry))

            val ackReceived = serverSocketHelper.send(outgoingPayload, timeout = 5.seconds)
            assertTrue("Expected server to receive ACK for work payload", ackReceived)

            val persisted = repository.get("work-entry-1")
            assertNotNull(persisted)
            assertEquals("Note captured from Work Mac", persisted?.text)
            assertFalse("Work entries must be persisted with uploaded = false", persisted?.uploaded ?: true)
            assertFalse("Work entries must be persisted with replacesLocal = false", persisted?.replacesLocal ?: true)

            controllerJob.cancel()
            serverSocketHelper.stop()
            serverJob.cancel()
        }

    @Test
    fun testNeverSendsPastEntriesOrSyncRequests() =
        runBlocking {
            // Seed repository with an existing entry
            repository.insert(
                text = "Personal home journal entry",
                time = LocalDateTime.parse("2026-11-21T18:00:00"),
            )

            testWorkProfileManager.setProfile(
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

            val receivedEntities = mutableListOf<Entity>()
            serverSocketHelper.setEntityHandler { entity ->
                receivedEntities.add(entity)
            }

            val serverJob =
                launch(Dispatchers.IO) {
                    serverSocketHelper.start(host = "localhost", port = port)
                }
            serverSocketHelper.isConnected.first { it }

            val controller =
                WorkBridgeController(
                    repository = repository,
                    workProfileManager = testWorkProfileManager,
                    keyValueStore = workKeyValueStore,
                    json = testJson,
                    httpClient = httpClient,
                    retryDelay = 50.milliseconds,
                    maxRetries = 3,
                )

            val controllerJob = launch(Dispatchers.IO) { controller.start() }

            controller.state.first { it is WorkBridgeController.State.Connected }

            // Allow time for any hypothetical background sync to fire
            kotlinx.coroutines.delay(300)

            assertTrue(
                "Phone must never send past entries or sync requests to the Mac server",
                receivedEntities.isEmpty(),
            )

            controllerJob.cancel()
            serverSocketHelper.stop()
            serverJob.cancel()
        }

    @Test
    fun testRetryThreeTimesAndAutoStopOnFailure() =
        runBlocking {
            // Choose an unused port where no server is listening
            val unusedPort = ServerSocket(0).use { it.localPort }

            testWorkProfileManager.setProfile(
                WorkProfile(
                    host = "localhost",
                    port = unusedPort,
                    salt = testSalt,
                    password = testPassword,
                ),
            )

            val stopRequestedDeferred = CompletableDeferred<Unit>()
            val controller =
                WorkBridgeController(
                    repository = repository,
                    workProfileManager = testWorkProfileManager,
                    keyValueStore = workKeyValueStore,
                    json = testJson,
                    httpClient = httpClient,
                    retryDelay = 50.milliseconds,
                    maxRetries = 3,
                    onStopRequested = { stopRequestedDeferred.complete(Unit) },
                )

            val controllerJob = launch(Dispatchers.IO) { controller.start() }

            stopRequestedDeferred.await()
            assertEquals(WorkBridgeController.State.Stopped, controller.state.value)
        }

    @Test
    fun testDisconnectAfterConnectionRetriesAndAutoStops() =
        runBlocking {
            testWorkProfileManager.setProfile(
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
                    workProfileManager = testWorkProfileManager,
                    keyValueStore = workKeyValueStore,
                    json = testJson,
                    httpClient = httpClient,
                    retryDelay = 50.milliseconds,
                    maxRetries = 3,
                    onStopRequested = { stopRequestedDeferred.complete(Unit) },
                )

            val controllerJob = launch(Dispatchers.IO) { controller.start() }

            controller.state.first { it is WorkBridgeController.State.Connected }

            // Stop server while client is connected
            server?.stop(50, 100)
            server = null

            // Controller should retry 3 times and then request stop
            stopRequestedDeferred.await()
            assertEquals(WorkBridgeController.State.Stopped, controller.state.value)
        }

    @Test
    fun testCancellationTerminatesImmediately() =
        runBlocking {
            val unusedPort = ServerSocket(0).use { it.localPort }
            testWorkProfileManager.setProfile(
                WorkProfile(
                    host = "localhost",
                    port = unusedPort,
                    salt = testSalt,
                    password = testPassword,
                ),
            )

            val controller =
                WorkBridgeController(
                    repository = repository,
                    workProfileManager = testWorkProfileManager,
                    keyValueStore = workKeyValueStore,
                    json = testJson,
                    httpClient = httpClient,
                    retryDelay = 5.seconds,
                    maxRetries = 3,
                )

            val controllerJob = launch(Dispatchers.IO) { controller.start() }

            controller.state.first { it is WorkBridgeController.State.Connecting }
            controllerJob.cancel()

            controller.state.first { it is WorkBridgeController.State.Stopped }
            assertEquals(WorkBridgeController.State.Stopped, controller.state.value)
        }

    @Test
    fun testMissingProfileStopsImmediately() =
        runBlocking {
            testWorkProfileManager.clearProfile()

            val stopRequestedDeferred = CompletableDeferred<Unit>()
            val controller =
                WorkBridgeController(
                    repository = repository,
                    workProfileManager = testWorkProfileManager,
                    keyValueStore = workKeyValueStore,
                    json = testJson,
                    httpClient = httpClient,
                    onStopRequested = { stopRequestedDeferred.complete(Unit) },
                )

            val controllerJob = launch(Dispatchers.IO) { controller.start() }

            stopRequestedDeferred.await()
            assertEquals(WorkBridgeController.State.Stopped, controller.state.value)
        }

    @Test
    fun testCredentialIsolation() =
        runBlocking {
            // Set home sync credentials
            workKeyValueStore.putString(Key.ENCRYPTION_PASSWORD, "home-password")
            workKeyValueStore.putString(Key.ENCRYPTION_SALT, "home-salt")

            // Set work credentials
            testWorkProfileManager.setProfile(
                WorkProfile(
                    host = "localhost",
                    port = port,
                    salt = testSalt,
                    password = testPassword,
                ),
            )

            assertEquals("home-password", workKeyValueStore.getString(Key.ENCRYPTION_PASSWORD, ""))
            assertEquals("home-salt", workKeyValueStore.getString(Key.ENCRYPTION_SALT, ""))
            assertEquals(testPassword, workKeyValueStore.getString(Key.WORK_PASSWORD, ""))
            assertEquals(testSalt, workKeyValueStore.getString(Key.WORK_SALT, ""))
        }
}
