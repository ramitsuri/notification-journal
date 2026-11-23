import Testing
import Foundation
@testable import WorkMacCore

@MainActor
struct HUDViewModelTests {

    @Test func submitWhileDisconnectedRejectsAndShakesWithoutClearingText() async {
        let didCallSend = StateBox(false)
        let vm = HUDViewModel(
            sendHandler: { _ in
                didCallSend.set(true)
                return "ack-id"
            },
            isConnected: { false }
        )

        vm.inputText = "My important quick thought"
        let initialShake = vm.shakeTrigger

        vm.submit()

        #expect(vm.inputText == "My important quick thought")
        #expect(vm.shakeTrigger == initialShake + 1)
        #expect(vm.errorMessage != nil)
        #expect(vm.inFlightItems.isEmpty)
        #expect(didCallSend.current == false)
    }

    @Test func submitWhileConnectedClearsInputAndEnqueuesPendingItem() async {
        let sentEntry = StateBox<JournalEntry?>(nil)

        let vm = HUDViewModel(
            sendHandler: { entry in
                sentEntry.set(entry)
                return "ack-123"
            },
            isConnected: { true },
            fadeDelay: 10.0
        )

        vm.inputText = "Ship issue 07"
        vm.submit()

        #expect(vm.inputText == "")
        #expect(vm.errorMessage == nil)
        #expect(vm.inFlightItems.count == 1)

        let item = vm.inFlightItems.first
        #expect(item?.text == "Ship issue 07")
        #expect(item?.status == .pending)
    }

    @Test func successfulSendTransitionsItemToAcknowledgedAndAutoFades() async throws {
        let vm = HUDViewModel(
            sendHandler: { entry in
                return "ack-456"
            },
            isConnected: { true },
            fadeDelay: 0.05
        )

        vm.inputText = "Quick thought"
        vm.submit()

        #expect(vm.inFlightItems.count == 1)

        // Wait for send task to complete and set status to acknowledged
        for _ in 0..<20 {
            if vm.inFlightItems.first?.status == .acknowledged { break }
            try await Task.sleep(nanoseconds: 20_000_000)
        }
        #expect(vm.inFlightItems.first?.status == .acknowledged)

        // Wait for auto-fade delay (0.05s) to remove item from inFlightItems
        for _ in 0..<20 {
            if vm.inFlightItems.isEmpty { break }
            try await Task.sleep(nanoseconds: 20_000_000)
        }
        #expect(vm.inFlightItems.isEmpty)
    }

    @Test func failedSendTransitionsItemToFailedExposingRetryAndDiscard() async throws {
        enum DummyError: LocalizedError {
            case networkTimeout
            var errorDescription: String? { "Network timed out" }
        }

        let vm = HUDViewModel(
            sendHandler: { _ in
                throw DummyError.networkTimeout
            },
            isConnected: { true }
        )

        vm.inputText = "Failing thought"
        vm.submit()

        #expect(vm.inFlightItems.count == 1)

        // Wait for send to fail
        for _ in 0..<20 {
            if case .failed = vm.inFlightItems.first?.status { break }
            try await Task.sleep(nanoseconds: 20_000_000)
        }

        guard let firstItem = vm.inFlightItems.first else {
            Issue.record("Expected in-flight item")
            return
        }

        if case .failed(let message) = firstItem.status {
            #expect(message == "Network timed out")
        } else {
            Issue.record("Expected failed status, got \(firstItem.status)")
        }

        // Discard should remove the item
        vm.discard(id: firstItem.id)
        #expect(vm.inFlightItems.isEmpty)
    }

    @Test func retryFailedItemWhenConnectedResendsAndAcknowledges() async throws {
        let shouldFail = StateBox(true)
        let sendCount = StateBox(0)

        let vm = HUDViewModel(
            sendHandler: { entry in
                sendCount.set(sendCount.current + 1)
                if shouldFail.current {
                    throw WorkServerError.connectionLost
                }
                return "retry-ack"
            },
            isConnected: { true },
            fadeDelay: 10.0
        )

        vm.inputText = "Retried thought"
        vm.submit()

        for _ in 0..<20 {
            if case .failed = vm.inFlightItems.first?.status { break }
            try await Task.sleep(nanoseconds: 20_000_000)
        }
        #expect(sendCount.current == 1)

        guard let failedItem = vm.inFlightItems.first else {
            Issue.record("Expected item")
            return
        }

        // Now fix connection and retry
        shouldFail.set(false)
        vm.retry(id: failedItem.id)

        for _ in 0..<20 {
            if vm.inFlightItems.first?.status == .acknowledged { break }
            try await Task.sleep(nanoseconds: 20_000_000)
        }

        #expect(sendCount.current == 2)
        #expect(vm.inFlightItems.first?.status == .acknowledged)
    }

    @Test func retryWhileDisconnectedRejectsAndLeavesItemFailed() async throws {
        let connected = StateBox(true)
        let vm = HUDViewModel(
            sendHandler: { _ in
                throw WorkServerError.timedOut
            },
            isConnected: { connected.current }
        )

        vm.inputText = "Thought to retry"
        vm.submit()

        for _ in 0..<20 {
            if case .failed = vm.inFlightItems.first?.status { break }
            try await Task.sleep(nanoseconds: 20_000_000)
        }

        guard let item = vm.inFlightItems.first else {
            Issue.record("Expected item")
            return
        }

        connected.set(false)
        let initialShake = vm.shakeTrigger
        vm.retry(id: item.id)

        #expect(vm.shakeTrigger == initialShake + 1)
        #expect(vm.errorMessage != nil)
        #expect(vm.inFlightItems.first?.status == item.status)
    }

    @Test func dismissLeavesInFlightItemsIntactInMemory() async throws {
        var didDismiss = false
        let vm = HUDViewModel(
            sendHandler: { _ in
                throw WorkServerError.timedOut
            },
            isConnected: { true },
            dismissHandler: {
                didDismiss = true
            }
        )

        vm.inputText = "Persistent in-flight thought"
        vm.submit()

        for _ in 0..<20 {
            if case .failed = vm.inFlightItems.first?.status { break }
            try await Task.sleep(nanoseconds: 20_000_000)
        }

        #expect(vm.inFlightItems.count == 1)

        vm.dismiss()

        #expect(didDismiss == true)
        #expect(vm.inFlightItems.count == 1)
        #expect(vm.inFlightItems.first?.text == "Persistent in-flight thought")
    }

    @Test func endToEndServerAndHUDViewModelSendAndAck() async throws {
        let creds = SessionCredentials()
        let appState = WorkAppState(credentials: creds, requestedPort: 0, startServerImmediately: true)
        let clientEngine = try CryptoEngine(credentials: creds)

        // Wait for port assignment
        var assignedPort: UInt16? = nil
        for _ in 0..<20 {
            if appState.port != 0 {
                assignedPort = appState.port
                break
            }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
        let port = try #require(assignedPort)

        // Connect client using URLSessionWebSocketTask
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

        let vm = HUDViewModel(appState: appState, fadeDelay: 0.5)
        vm.inputText = "End-to-end captured thought"
        vm.submit()

        #expect(vm.inFlightItems.count == 1)
        #expect(vm.inFlightItems.first?.status == .pending)

        // Client receives incoming encrypted message
        let received = try await wsTask.receive()
        guard case .data(let cipherData) = received else {
            Issue.record("Expected binary WebSocket frame")
            return
        }

        let decryptedJson = try clientEngine.decrypt(cipherData: cipherData)
        let message = try JSONDecoder().decode(OutgoingDataMessage.self, from: Data(decryptedJson.utf8))
        #expect(message.payload.data.first?.text == "End-to-end captured thought")

        // Client sends ACK
        let ack = IncomingAck(id: message.id)
        let ackJson = try JSONEncoder().encode(ack)
        let encryptedAck = try clientEngine.encrypt(plaintext: String(data: ackJson, encoding: .utf8)!)
        try await wsTask.send(.data(encryptedAck))

        // VM item transitions to acknowledged
        for _ in 0..<20 {
            if vm.inFlightItems.first?.status == .acknowledged { break }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
        #expect(vm.inFlightItems.first?.status == .acknowledged)

        // And then fades away
        for _ in 0..<20 {
            if vm.inFlightItems.isEmpty { break }
            try await Task.sleep(nanoseconds: 50_000_000)
        }
        #expect(vm.inFlightItems.isEmpty)

        Task { await appState.server.stop() }
    }
}

private final class StateBox<T>: @unchecked Sendable {
    private let lock = NSLock()
    private var value: T

    init(_ value: T) {
        self.value = value
    }

    var current: T {
        lock.lock()
        defer { lock.unlock() }
        return value
    }

    func set(_ newValue: T) {
        lock.lock()
        defer { lock.unlock() }
        self.value = newValue
    }
}
