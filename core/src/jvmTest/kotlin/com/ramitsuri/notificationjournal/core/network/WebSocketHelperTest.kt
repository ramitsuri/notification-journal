package com.ramitsuri.notificationjournal.core.network

import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Entity
import com.ramitsuri.notificationjournal.core.model.sync.Message
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import com.ramitsuri.notificationjournal.core.model.sync.Sender
import com.ramitsuri.notificationjournal.core.model.sync.SyncRequest
import com.ramitsuri.notificationjournal.core.model.sync.VerifyEntries
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.EncryptionHelper
import com.ramitsuri.notificationjournal.core.utils.getTestDataKeyValueStore
import com.ramitsuri.notificationjournal.core.utils.testJson
import com.ramitsuri.notificationjournal.server.module
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.send
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.deleteRecursively
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WebSocketHelperTest {
    private var port: Int = 0
    private var server: EmbeddedServer<*, *>? = null
    private lateinit var httpClient: HttpClient
    private lateinit var encryptionHelper: EncryptionHelper
    private val dataHostProps =
        DataHostProperties(
            dataHost = "localhost",
            deviceName = "TestDevice",
            deviceId = "test-id",
        )

    @Before
    fun setUp() {
        port = ServerSocket(0).use { it.localPort }
        server =
            embeddedServer(Netty, port = port, module = Application::module)
                .start(wait = false)

        httpClient =
            HttpClient {
                install(WebSockets)
            }
        val keyValueStore = getTestDataKeyValueStore()
        encryptionHelper = EncryptionHelper(keyValueStore)
        runBlocking {
            encryptionHelper.resetValues(overwrite = false)
        }
    }

    @After
    fun tearDown() {
        server?.stop(100, 200)
        server = null
        httpClient.close()
        Paths.get(Constants.BASE_DIR).deleteRecursively()
    }

    @Test
    fun testSendAndAckSuccess() =
        runBlocking {
            val sender = WebSocketHelper(httpClient, testJson, encryptionHelper, port = port)
            val receiver = WebSocketHelper(httpClient, testJson, encryptionHelper, port = port)

            val receivedEntityDeferred = CompletableDeferred<Entity>()
            receiver.setEntityHandler { entity ->
                receivedEntityDeferred.complete(entity)
            }

            val senderJob = launch(Dispatchers.IO) { sender.start { dataHostProps } }
            val receiverJob = launch(Dispatchers.IO) { receiver.start { dataHostProps } }

            sender.isConnected.first { it }
            receiver.isConnected.first { it }

            val payload = sampleEntity()
            val sent = sender.send(payload, timeout = 5.seconds)

            assertTrue(sent)
            val received = receivedEntityDeferred.await()
            assertEquals(payload, received)

            sender.stop()
            receiver.stop()
            senderJob.cancel()
            receiverJob.cancel()
        }

    @Test
    fun testStartWithExplicitHostAndPort() =
        runBlocking {
            val receiver = WebSocketHelper(httpClient, testJson, encryptionHelper, port = 0)
            val receiverJob = launch(Dispatchers.IO) { receiver.start(host = "localhost", port = port) }

            receiver.isConnected.first { it }
            assertTrue(receiver.isConnected.value)

            receiver.stop()
            receiverJob.cancel()
        }

    @Test
    fun testSendTimeoutWithoutAck() =
        runBlocking {
            val sender = WebSocketHelper(httpClient, testJson, encryptionHelper, port = port)
            val senderJob = launch(Dispatchers.IO) { sender.start { dataHostProps } }

            sender.isConnected.first { it }

            val payload = sampleEntity()
            val sent = sender.send(payload, timeout = 200.milliseconds)

            assertFalse(sent)

            sender.stop()
            senderJob.cancel()
        }

    @Test
    fun testFastFailOnDisconnect() =
        runBlocking {
            val sender = WebSocketHelper(httpClient, testJson, encryptionHelper, port = port)
            val senderJob = launch(Dispatchers.IO) { sender.start { dataHostProps } }

            sender.isConnected.first { it }

            val payload = sampleEntity()
            val sendDeferred =
                async(Dispatchers.IO) {
                    sender.send(payload, timeout = 10.seconds)
                }

            delay(100.milliseconds)
            val startTime = System.currentTimeMillis()
            sender.stop()

            val result = sendDeferred.await()
            val elapsed = System.currentTimeMillis() - startTime

            assertFalse(result)
            assertTrue("Expected fast-fail (<5s), took ${elapsed}ms", elapsed < 5000)

            senderJob.cancel()
        }

    @Test
    fun testDeduplicationAndReAck() =
        runBlocking {
            val receiver = WebSocketHelper(httpClient, testJson, encryptionHelper, port = port)
            val callCount = AtomicInteger(0)
            receiver.setEntityHandler {
                callCount.incrementAndGet()
            }

            val receiverJob = launch(Dispatchers.IO) { receiver.start { dataHostProps } }
            receiver.isConnected.first { it }

            httpClient.webSocket(
                method = HttpMethod.Get,
                host = "localhost",
                port = port,
                path = "/${Constants.DATA_HOST_EXCHANGE}",
            ) {
                val testId = "dup-message-id"
                val message: Message = Message.Data(id = testId, payload = sampleEntity())
                val encrypted = encryptionHelper.encrypt(testJson.encodeToString<Message>(message))

                // Send 1st delivery
                send(encrypted)
                val response1 = incoming.receive() as Frame.Binary
                val ack1 = testJson.decodeFromString<Message>(encryptionHelper.decrypt(response1.readBytes()))
                assertTrue(ack1 is Message.Ack)
                assertEquals(testId, (ack1 as Message.Ack).id)

                // Send 2nd delivery (duplicate)
                send(encrypted)
                val response2 = incoming.receive() as Frame.Binary
                val ack2 = testJson.decodeFromString<Message>(encryptionHelper.decrypt(response2.readBytes()))
                assertTrue(ack2 is Message.Ack)
                assertEquals(testId, (ack2 as Message.Ack).id)
            }

            assertEquals(1, callCount.get())

            receiver.stop()
            receiverJob.cancel()
        }

    @Test
    fun testNonAckableDiagnostics() =
        runBlocking {
            val sender = WebSocketHelper(httpClient, testJson, encryptionHelper, port = port)
            val senderJob = launch(Dispatchers.IO) { sender.start { dataHostProps } }

            sender.isConnected.first { it }

            val verifyReq =
                VerifyEntries.Request(
                    date = LocalDate(2025, 1, 1),
                    verification = VerifyEntries.Verification(emptyList()),
                    time = Clock.System.now(),
                )
            val reqSent = sender.send(verifyReq, timeout = 1.seconds)
            assertTrue(reqSent)

            val syncReq = SyncRequest(time = Clock.System.now())
            val syncSent = sender.send(syncReq, timeout = 1.seconds)
            assertTrue(syncSent)

            sender.stop()
            senderJob.cancel()
        }

    @Test
    fun testFirstAckWins() =
        runBlocking {
            val sender = WebSocketHelper(httpClient, testJson, encryptionHelper, port = port)
            val senderJob = launch(Dispatchers.IO) { sender.start { dataHostProps } }
            sender.isConnected.first { it }

            val peer1Connected = CompletableDeferred<Unit>()
            val peer2Connected = CompletableDeferred<Unit>()

            val peer1Job = launchAckResponder(peer1Connected)
            val peer2Job = launchAckResponder(peer2Connected)

            peer1Connected.await()
            peer2Connected.await()

            val payload = sampleEntity()
            val sent = sender.send(payload, timeout = 5.seconds)
            assertTrue(sent)

            sender.stop()
            senderJob.cancel()
            peer1Job.cancel()
            peer2Job.cancel()
        }

    @Test
    fun testSyncRequestDoesNotDeadlockReceiver() =
        runBlocking {
            val peerA = WebSocketHelper(httpClient, testJson, encryptionHelper, port = port)
            val peerB = WebSocketHelper(httpClient, testJson, encryptionHelper, port = port)

            val peerAEntityReceived = CompletableDeferred<Entity>()
            peerA.setEntityHandler { entity ->
                peerAEntityReceived.complete(entity)
            }

            val peerBJob = launch(Dispatchers.IO) { peerB.start { dataHostProps } }
            val peerAJob = launch(Dispatchers.IO) { peerA.start { dataHostProps } }

            peerA.isConnected.first { it }
            peerB.isConnected.first { it }

            // When Peer B receives SyncRequest on its messages flow, launch an upload back to Peer A
            val peerBUploadSuccess = CompletableDeferred<Boolean>()
            val syncRequestJob =
                launch(Dispatchers.IO) {
                    peerB.messages
                        .first { it is SyncRequest }
                    // Trigger upload asynchronously, as AppLifecycleCoordinator does
                    launch(Dispatchers.IO) {
                        val uploaded = peerB.send(sampleEntity(), timeout = 5.seconds)
                        peerBUploadSuccess.complete(uploaded)
                    }
                }

            // Peer A broadcasts SyncRequest
            val syncSent = peerA.send(SyncRequest(time = Clock.System.now()), timeout = 5.seconds)
            assertTrue(syncSent)

            // Peer B's upload must succeed and not deadlock waiting for Peer A's ACK
            val uploadResult = peerBUploadSuccess.await()
            assertTrue("Expected Peer B upload to succeed without deadlock", uploadResult)
            assertEquals(sampleEntity(), peerAEntityReceived.await())

            peerA.stop()
            peerB.stop()
            peerAJob.cancel()
            peerBJob.cancel()
            syncRequestJob.cancel()
        }

    private fun CoroutineScope.launchAckResponder(connected: CompletableDeferred<Unit>): Job =
        launch(Dispatchers.IO) {
            httpClient.webSocket(
                method = HttpMethod.Get,
                host = "localhost",
                port = port,
                path = "/${Constants.DATA_HOST_EXCHANGE}",
            ) {
                connected.complete(Unit)
                for (incomingMessage in incoming) {
                    incomingMessage as? Frame.Binary ?: continue
                    val decrypted = encryptionHelper.decrypt(incomingMessage.readBytes())
                    val msg = testJson.decodeFromString<Message>(decrypted) as? Message.Data ?: continue
                    val ack: Message = Message.Ack(id = msg.id!!)
                    send(encryptionHelper.encrypt(testJson.encodeToString<Message>(ack)))
                    break
                }
            }
        }

    @Test
    fun testLegacyRawPayloadFallback() =
        runBlocking {
            val receiver = WebSocketHelper(httpClient, testJson, encryptionHelper, port = port)
            val receivedEntityDeferred = CompletableDeferred<Entity>()
            receiver.setEntityHandler { entity ->
                receivedEntityDeferred.complete(entity)
            }

            val receiverJob = launch(Dispatchers.IO) { receiver.start { dataHostProps } }
            receiver.isConnected.first { it }

            httpClient.webSocket(
                method = HttpMethod.Get,
                host = "localhost",
                port = port,
                path = "/${Constants.DATA_HOST_EXCHANGE}",
            ) {
                val payload: Payload = sampleEntity()
                val rawPayloadJson = testJson.encodeToString<Payload>(payload)
                send(encryptionHelper.encrypt(rawPayloadJson))
            }

            val received = receivedEntityDeferred.await()
            assertEquals(sampleEntity(), received)

            receiver.stop()
            receiverJob.cancel()
        }

    private fun sampleEntity(): Entity.Entries =
        Entity.Entries(
            data =
                listOf(
                    JournalEntry(
                        id = "entry-1",
                        entryTime = LocalDateTime.parse("2024-12-25T12:00:00"),
                        text = "Sample test entry",
                        tag = Tag.NO_TAG.value,
                        uploaded = false,
                        replacesLocal = false,
                        deleted = false,
                        reconciled = false,
                    ),
                ),
            sender = Sender(name = "Peer1", id = "peer-1"),
        )
}
