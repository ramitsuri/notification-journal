import Foundation
import Network

public enum WorkServerError: Error, Equatable, Sendable {
    case notStarted
    case alreadyStarted
    case notConnected
    case timedOut
    case connectionLost
    case serializationFailed
    case encryptionFailed
    case listenerFailed(String)
}

public actor WorkWebSocketServer {
    private let queue = DispatchQueue(label: "com.notificationjournal.work-mac.server", qos: .userInitiated)

    private var _credentials: SessionCredentials
    private var cryptoEngine: CryptoEngine
    private let requestedPort: UInt16

    private var listener: NWListener?
    private var _port: UInt16?
    private var startContinuation: CheckedContinuation<Void, Error>?
    private var activeConnections: [NWConnection] = []
    private var pendingAcks: [String: CheckedContinuation<String, Error>] = [:]

    private var statusChangeHandler: (@Sendable (Bool) -> Void)?

    public var credentials: SessionCredentials {
        _credentials
    }

    public var port: UInt16? {
        _port
    }

    public var isClientConnected: Bool {
        !activeConnections.isEmpty
    }

    public init(credentials: SessionCredentials, port: UInt16 = 8080) throws {
        self._credentials = credentials
        self.cryptoEngine = try CryptoEngine(credentials: credentials)
        self.requestedPort = port
    }

    public func setStatusChangeHandler(_ handler: (@Sendable (Bool) -> Void)?) {
        self.statusChangeHandler = handler
    }

    public func start() async throws {
        if listener != nil {
            throw WorkServerError.alreadyStarted
        }

        let parameters = NWParameters.tcp
        let wsOptions = NWProtocolWebSocket.Options()
        parameters.defaultProtocolStack.applicationProtocols.insert(wsOptions, at: 0)

        let primaryPort: NWEndpoint.Port = requestedPort == 0 ? .any : NWEndpoint.Port(rawValue: requestedPort)!
        let newListener: NWListener
        do {
            newListener = try NWListener(using: parameters, on: primaryPort)
        } catch {
            // Fall back to an available port if requested port fails to bind
            do {
                newListener = try NWListener(using: parameters, on: .any)
            } catch {
                throw WorkServerError.listenerFailed(error.localizedDescription)
            }
        }
        self.listener = newListener

        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            self.startContinuation = continuation

            newListener.stateUpdateHandler = { [weak self, weak newListener] state in
                guard let self = self, let listener = newListener else { return }
                Task {
                    await self.handleListenerStateUpdate(state: state, listener: listener)
                }
            }

            newListener.newConnectionHandler = { [weak self] connection in
                guard let self = self else { return }
                Task {
                    await self.handleNewConnection(connection)
                }
            }

            newListener.start(queue: queue)
        }
    }

    private func handleListenerStateUpdate(state: NWListener.State, listener: NWListener) {
        switch state {
        case .ready:
            self._port = listener.port?.rawValue
            if let continuation = self.startContinuation {
                self.startContinuation = nil
                continuation.resume()
            }
        case .failed(let error):
            if let continuation = self.startContinuation {
                self.startContinuation = nil
                continuation.resume(throwing: WorkServerError.listenerFailed(error.localizedDescription))
            }
        default:
            break
        }
    }

    public func stop() {
        if let continuation = startContinuation {
            startContinuation = nil
            continuation.resume(throwing: WorkServerError.connectionLost)
        }

        listener?.cancel()
        listener = nil
        _port = nil

        teardownConnectionsAndPendingAcks()
    }

    public func rotateCredentials(credentials: SessionCredentials = SessionCredentials()) throws {
        self._credentials = credentials
        self.cryptoEngine = try CryptoEngine(credentials: credentials)

        teardownConnectionsAndPendingAcks()
    }

    private func teardownConnectionsAndPendingAcks() {
        let connectionsToClose = activeConnections
        activeConnections.removeAll()

        let pendingToFail = pendingAcks.values
        pendingAcks.removeAll()

        for conn in connectionsToClose {
            conn.cancel()
        }

        for cont in pendingToFail {
            cont.resume(throwing: WorkServerError.connectionLost)
        }

        statusChangeHandler?(false)
    }

    public func send(entry: JournalEntry, timeout: TimeInterval = 10.0) async throws -> String {
        guard !activeConnections.isEmpty else {
            throw WorkServerError.notConnected
        }

        let messageId = UUID().uuidString.lowercased()
        let message = OutgoingDataMessage(
            id: messageId,
            payload: EntriesPayload(data: [entry])
        )

        let encoder = JSONEncoder()
        let jsonData: Data
        do {
            jsonData = try encoder.encode(message)
        } catch {
            throw WorkServerError.serializationFailed
        }

        guard let jsonString = String(data: jsonData, encoding: .utf8) else {
            throw WorkServerError.serializationFailed
        }

        let encrypted: Data
        do {
            encrypted = try cryptoEngine.encrypt(plaintext: jsonString)
        } catch {
            throw WorkServerError.encryptionFailed
        }

        let metadata = NWProtocolWebSocket.Metadata(opcode: .binary)
        let context = NWConnection.ContentContext(identifier: "WorkWebSocketBinary", metadata: [metadata])

        for conn in activeConnections {
            conn.send(content: encrypted, contentContext: context, isComplete: true, completion: .contentProcessed { _ in })
        }

        return try await withCheckedThrowingContinuation { continuation in
            self.pendingAcks[messageId] = continuation

            Task { [weak self] in
                try? await Task.sleep(nanoseconds: UInt64(timeout * 1_000_000_000))
                await self?.timeoutAck(messageId: messageId)
            }
        }
    }

    private func timeoutAck(messageId: String) {
        if let cont = pendingAcks.removeValue(forKey: messageId) {
            cont.resume(throwing: WorkServerError.timedOut)
        }
    }

    private func handleNewConnection(_ connection: NWConnection) {
        connection.stateUpdateHandler = { [weak self, weak connection] state in
            guard let self = self, let connection = connection else { return }
            Task {
                await self.handleConnectionStateChange(connection: connection, state: state)
            }
        }
        connection.start(queue: queue)
    }

    private func handleConnectionStateChange(connection: NWConnection, state: NWConnection.State) {
        switch state {
        case .ready:
            activeConnections.append(connection)
            statusChangeHandler?(true)
            receiveLoop(connection: connection)
        case .failed, .cancelled:
            activeConnections.removeAll { $0 === connection }
            if activeConnections.isEmpty {
                let toFail = Array(pendingAcks.values)
                pendingAcks.removeAll()
                for cont in toFail {
                    cont.resume(throwing: WorkServerError.connectionLost)
                }
                statusChangeHandler?(false)
            }
        default:
            break
        }
    }

    private nonisolated func receiveLoop(connection: NWConnection) {
        connection.receiveMessage { [weak self] content, context, isComplete, error in
            guard let self = self else { return }
            if error != nil {
                return
            }

            if let content = content, !content.isEmpty {
                Task {
                    await self.handleIncomingData(content)
                }
            }

            self.receiveLoop(connection: connection)
        }
    }

    private func handleIncomingData(_ data: Data) {
        guard let decryptedString = try? cryptoEngine.decrypt(cipherData: data),
              let jsonData = decryptedString.data(using: .utf8),
              let ack = try? JSONDecoder().decode(IncomingAck.self, from: jsonData),
              ack.type == "ack" else {
            return
        }

        if let continuation = pendingAcks.removeValue(forKey: ack.id) {
            continuation.resume(returning: ack.id)
        }
    }
}
