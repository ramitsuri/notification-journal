import Testing
import Foundation
@testable import WorkMacCore

@MainActor
struct WorkQuickCaptureSystemIntegrationTests {

    @Test func testEndToEndPairingStreamAndAckFlow() async throws {
        let creds = SessionCredentials()
        let appState = WorkAppState(credentials: creds, requestedPort: 0, startServerImmediately: true)
        let clientEngine = try CryptoEngine(credentials: creds)

        // Verify pairing QR code string contains expected work namespace
        #expect(appState.pairingString.hasPrefix("work;;;"))
        #expect(appState.pairingString.contains(";;;\(creds.salt);;;"))
        #expect(appState.pairingString.contains(";;;\(creds.password);;;"))

        // Wait for server to bind dynamic port
        var assignedPort: UInt16? = nil
        for _ in 0..<20 {
            if appState.port != 0 {
                assignedPort = appState.port
                break
            }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
        let port = try #require(assignedPort)

        // Connect client (simulating Android app)
        let session = URLSession(configuration: .default)
        let url = URL(string: "ws://127.0.0.1:\(port)/Exchange")!
        let wsTask = session.webSocketTask(with: url)
        wsTask.resume()
        defer { wsTask.cancel(with: .normalClosure, reason: nil) }

        for _ in 0..<20 {
            if appState.isClientConnected { break }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
        #expect(appState.isClientConnected == true)

        let vm = HUDViewModel(appState: appState, fadeDelay: 0.2)
        vm.inputText = "E2E Quick capture note from work Mac"
        vm.submit()

        // Input clears immediately and pending item enqueued
        #expect(vm.inputText == "")
        #expect(vm.inFlightItems.count == 1)
        #expect(vm.inFlightItems.first?.status == .pending)

        // Android client receives binary frame
        let received = try await wsTask.receive()
        guard case .data(let cipherData) = received else {
            Issue.record("Expected binary WebSocket frame")
            return
        }

        // Decrypt payload
        let decryptedJson = try clientEngine.decrypt(cipherData: cipherData)
        let message = try JSONDecoder().decode(OutgoingDataMessage.self, from: Data(decryptedJson.utf8))
        #expect(message.type == "data")
        #expect(message.payload.sender.name == "Work Mac")
        #expect(message.payload.sender.id == "work-mac")
        #expect(message.payload.data.count == 1)

        let entry = try #require(message.payload.data.first)
        #expect(entry.text == "E2E Quick capture note from work Mac")
        #expect(entry.uploaded == false)
        #expect(entry.autoTagged == false)
        #expect(entry.deleted == false)
        #expect(entry.reconciled == false)

        // Verify entryTime format has no timezone offset or Z
        #expect(!entry.entryTime.contains("Z"))
        #expect(!entry.entryTime.contains("+"))

        // Send encrypted ACK back to Mac
        let ack = IncomingAck(id: message.id)
        let ackJson = try JSONEncoder().encode(ack)
        let encryptedAck = try clientEngine.encrypt(plaintext: String(data: ackJson, encoding: .utf8)!)
        try await wsTask.send(.data(encryptedAck))

        // In-flight status transitions to acknowledged
        for _ in 0..<20 {
            if vm.inFlightItems.first?.status == .acknowledged { break }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
        #expect(vm.inFlightItems.first?.status == .acknowledged)

        // Auto-fades after fadeDelay
        for _ in 0..<20 {
            if vm.inFlightItems.isEmpty { break }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
        #expect(vm.inFlightItems.isEmpty)

        await appState.server.stop()
    }

    @Test func testDisconnectionDropAndReconnectionRecovery() async throws {
        let creds = SessionCredentials()
        let appState = WorkAppState(credentials: creds, requestedPort: 0, startServerImmediately: true)
        let clientEngine = try CryptoEngine(credentials: creds)

        var assignedPort: UInt16? = nil
        for _ in 0..<20 {
            if appState.port != 0 {
                assignedPort = appState.port
                break
            }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
        let port = try #require(assignedPort)

        let session = URLSession(configuration: .default)
        let url = URL(string: "ws://127.0.0.1:\(port)/Exchange")!
        var wsTask = session.webSocketTask(with: url)
        wsTask.resume()

        for _ in 0..<20 {
            if appState.isClientConnected { break }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
        #expect(appState.isClientConnected == true)

        let vm = HUDViewModel(appState: appState, fadeDelay: 0.2)

        // 1. Submit note to test discard
        vm.inputText = "Note to discard on failure"
        vm.submit()
        let discardItemId = try #require(vm.inFlightItems.first?.id)

        // Disconnect phone before sending ACK to trigger failure
        wsTask.cancel(with: .goingAway, reason: nil)

        for _ in 0..<20 {
            if !appState.isClientConnected { break }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
        #expect(appState.isClientConnected == false)

        // The in-flight item fails when socket drops without ACK
        for _ in 0..<20 {
            if case .failed = vm.inFlightItems.first(where: { $0.id == discardItemId })?.status { break }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
        guard case .failed = vm.inFlightItems.first(where: { $0.id == discardItemId })?.status else {
            Issue.record("Expected item to transition to failed")
            return
        }

        // Test Discard action removes the failed note
        vm.discard(id: discardItemId)
        #expect(vm.inFlightItems.isEmpty)

        // 2. Submission while disconnected rejects immediately with shake and error prompt
        vm.inputText = "Should fail when disconnected"
        let initialShake = vm.shakeTrigger
        vm.submit()

        #expect(vm.shakeTrigger == initialShake + 1)
        #expect(vm.errorMessage != nil)
        #expect(vm.inputText == "Should fail when disconnected")
        #expect(vm.inFlightItems.isEmpty)

        // 3. Reconnect client
        wsTask = session.webSocketTask(with: url)
        wsTask.resume()
        defer { wsTask.cancel(with: .normalClosure, reason: nil) }

        for _ in 0..<20 {
            if appState.isClientConnected { break }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
        #expect(appState.isClientConnected == true)

        // Submit succeeds now that client reconnected
        vm.submit()
        #expect(vm.inFlightItems.count == 1)
        #expect(vm.inFlightItems.first?.status == .pending)

        let received = try await wsTask.receive()
        guard case .data(let cipherData) = received else {
            Issue.record("Expected binary WebSocket frame")
            return
        }

        let decryptedJson = try clientEngine.decrypt(cipherData: cipherData)
        let message = try JSONDecoder().decode(OutgoingDataMessage.self, from: Data(decryptedJson.utf8))
        #expect(message.payload.data.first?.text == "Should fail when disconnected")

        let ack = IncomingAck(id: message.id)
        let ackJson = try JSONEncoder().encode(ack)
        let encryptedAck = try clientEngine.encrypt(plaintext: String(data: ackJson, encoding: .utf8)!)
        try await wsTask.send(.data(encryptedAck))

        for _ in 0..<20 {
            if vm.inFlightItems.first?.status == .acknowledged { break }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
        #expect(vm.inFlightItems.first?.status == .acknowledged)

        await appState.server.stop()
    }

    @Test func testUsbModeAndReverseAdbFallback() async throws {
        let creds = SessionCredentials()
        let appState = WorkAppState(credentials: creds, requestedPort: 0, startServerImmediately: true)

        #expect(appState.isUsbMode == false)

        // Toggle USB mode
        appState.isUsbMode = true

        #expect(appState.currentHost == "127.0.0.1")
        #expect(appState.pairingString.contains(";;;127.0.0.1:"))
        #expect(appState.adbReverseCommand.contains("adb reverse tcp:"))

        var assignedPort: UInt16? = nil
        for _ in 0..<20 {
            if appState.port != 0 {
                assignedPort = appState.port
                break
            }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
        let port = try #require(assignedPort)

        let command = "adb reverse tcp:\(port) tcp:\(port)"
        #expect(appState.adbReverseCommand == command)

        // Connect over loopback (simulating adb reversed USB socket)
        let session = URLSession(configuration: .default)
        let url = URL(string: "ws://127.0.0.1:\(port)/Exchange")!
        let wsTask = session.webSocketTask(with: url)
        wsTask.resume()
        defer { wsTask.cancel(with: .normalClosure, reason: nil) }

        for _ in 0..<20 {
            if appState.isClientConnected { break }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
        #expect(appState.isClientConnected == true)

        await appState.server.stop()
    }

    @Test func testZeroDiskPersistenceGuarantee() {
        let fileManager = FileManager.default

        // Check current working directory for sqlite or db files
        let currentDir = fileManager.currentDirectoryPath
        let enumerator = fileManager.enumerator(atPath: currentDir)
        var foundDbFiles: [String] = []

        while let file = enumerator?.nextObject() as? String {
            let isDb = file.hasSuffix(".sqlite") || file.hasSuffix(".sqlite-wal")
                || file.hasSuffix(".db") || file.hasSuffix(".realm")
            if isDb && file.contains("work-mac") {
                foundDbFiles.append(file)
            }
        }
        #expect(foundDbFiles.isEmpty, "Found persistent database files in work-mac: \(foundDbFiles)")

        // Verify Application Support has no work quick-capture folder
        let appSupportUrls = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask)
        if let appSupport = appSupportUrls.first {
            let workDir = appSupport.appendingPathComponent("WorkMacApp")
            let exists = fileManager.fileExists(atPath: workDir.path)
            #expect(!exists, "WorkMacApp must not create Application Support files")
        }
    }
}
