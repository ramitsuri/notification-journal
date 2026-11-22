import Testing
import Foundation
import Network
@testable import WorkMacCore

struct WorkWebSocketServerTests {
    @Test func sendWithoutConnectedClientThrowsNotConnected() async throws {
        let creds = SessionCredentials()
        let server = try WorkWebSocketServer(credentials: creds, port: 0)
        try await server.start()
        defer {
            Task { await server.stop() }
        }

        let entry = JournalEntry(text: "Test Note")
        await #expect(throws: WorkServerError.notConnected) {
            _ = try await server.send(entry: entry, timeout: 1.0)
        }
    }

    @Test func sendAndAckCorrelationSuccess() async throws {
        let creds = SessionCredentials()
        let clientEngine = try CryptoEngine(credentials: creds)

        let server = try WorkWebSocketServer(credentials: creds, port: 0)
        try await server.start()
        defer {
            Task { await server.stop() }
        }

        guard let assignedPort = await server.port else {
            Issue.record("Server did not bind to a valid port")
            return
        }

        // Connect client using URLSessionWebSocketTask
        let session = URLSession(configuration: .default)
        let url = URL(string: "ws://127.0.0.1:\(assignedPort)/Exchange")!
        let wsTask = session.webSocketTask(with: url)
        wsTask.resume()
        defer { wsTask.cancel(with: .normalClosure, reason: nil) }

        // Wait briefly for connection to establish
        var connected = false
        for _ in 0..<20 {
            if await server.isClientConnected {
                connected = true
                break
            }
            try await Task.sleep(nanoseconds: 100_000_000) // 100ms
        }
        #expect(connected == true)

        let entry = JournalEntry(text: "Quick capture note from Mac")

        // Start send task in background
        let sendTask = Task {
            try await server.send(entry: entry, timeout: 5.0)
        }

        // Client receives incoming binary frame
        let receivedMessage = try await wsTask.receive()
        guard case .data(let cipherData) = receivedMessage else {
            Issue.record("Expected binary frame from server")
            return
        }

        // Client decrypts
        let decryptedJson = try clientEngine.decrypt(cipherData: cipherData)
        let decoder = JSONDecoder()
        let incomingMsg = try decoder.decode(OutgoingDataMessage.self, from: Data(decryptedJson.utf8))
        #expect(incomingMsg.type == "data")
        #expect(incomingMsg.payload.data.first?.text == "Quick capture note from Mac")

        // Client prepares and sends ACK
        let ack = IncomingAck(id: incomingMsg.id)
        let ackJsonData = try JSONEncoder().encode(ack)
        let encryptedAck = try clientEngine.encrypt(plaintext: String(data: ackJsonData, encoding: .utf8)!)

        try await wsTask.send(.data(encryptedAck))

        // Wait for server to correlate ACK
        let messageId = try await sendTask.value
        #expect(messageId == incomingMsg.id)
    }

    @Test func sendTimesOutWhenAckNotReceived() async throws {
        let creds = SessionCredentials()
        let server = try WorkWebSocketServer(credentials: creds, port: 0)
        try await server.start()
        defer {
            Task { await server.stop() }
        }

        guard let assignedPort = await server.port else {
            Issue.record("Server did not bind to a valid port")
            return
        }

        let session = URLSession(configuration: .default)
        let url = URL(string: "ws://127.0.0.1:\(assignedPort)/Exchange")!
        let wsTask = session.webSocketTask(with: url)
        wsTask.resume()
        defer { wsTask.cancel(with: .normalClosure, reason: nil) }

        for _ in 0..<20 {
            if await server.isClientConnected { break }
            try await Task.sleep(nanoseconds: 100_000_000)
        }
        #expect(await server.isClientConnected == true)

        let entry = JournalEntry(text: "Unacknowledged note")

        // Client receives message but intentionally does NOT send ACK
        let sendTask = Task {
            try await server.send(entry: entry, timeout: 0.5)
        }

        _ = try await wsTask.receive()

        await #expect(throws: WorkServerError.timedOut) {
            _ = try await sendTask.value
        }
    }
}
