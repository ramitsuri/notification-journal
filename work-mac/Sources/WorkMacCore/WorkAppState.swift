import AppKit
import Foundation

@MainActor
public final class WorkAppState: ObservableObject {
    @Published public private(set) var isClientConnected: Bool = false
    @Published public private(set) var credentials: SessionCredentials
    @Published public private(set) var port: UInt16
    @Published public private(set) var serverError: String?
    @Published public var selectedInterface: NetworkInterface?
    @Published public var isUsbMode: Bool = false
    @Published public private(set) var availableInterfaces: [NetworkInterface] = []
    @Published public var adbCopied: Bool = false

    public let server: WorkWebSocketServer

    public init(
        credentials: SessionCredentials = SessionCredentials(),
        requestedPort: UInt16 = 8080,
        interfaces: [NetworkInterface]? = nil,
        startServerImmediately: Bool = true
    ) {
        let serverInstance: WorkWebSocketServer
        if let s = try? WorkWebSocketServer(credentials: credentials, port: requestedPort) {
            serverInstance = s
            self.credentials = credentials
        } else {
            let fallbackCredentials = SessionCredentials()
            self.credentials = fallbackCredentials
            if let fallbackServer = try? WorkWebSocketServer(credentials: fallbackCredentials, port: requestedPort) {
                serverInstance = fallbackServer
            } else {
                // SecRandomCopyBytes credentials are valid, 0 port never fails on bind
                guard let zeroPortServer = try? WorkWebSocketServer(credentials: fallbackCredentials, port: 0) else {
                    fatalError("Unable to initialize server with ephemeral credentials")
                }
                serverInstance = zeroPortServer
            }
        }

        self.server = serverInstance
        self.port = requestedPort
        let ifaces = interfaces ?? NetworkUtility.getActiveInterfaces()
        self.availableInterfaces = ifaces
        self.selectedInterface = ifaces.first

        setupStatusHandler()

        if startServerImmediately {
            startServer()
        }
    }

    private func setupStatusHandler() {
        let server = self.server
        Task { [weak self] in
            await server.setStatusChangeHandler { [weak self] isConnected in
                Task { @MainActor [weak self] in
                    self?.isClientConnected = isConnected
                }
            }
        }
    }

    public func startServer() {
        Task {
            do {
                try await server.start()
                if let assignedPort = await server.port {
                    self.port = assignedPort
                }
                self.serverError = nil
            } catch WorkServerError.alreadyStarted {
                // Do nothing
            } catch {
                self.serverError = error.localizedDescription
            }
        }
    }

    public func rotateSecrets() async {
        let newCredentials = SessionCredentials()
        do {
            try await server.rotateCredentials(credentials: newCredentials)
            self.credentials = newCredentials
            self.isClientConnected = false
            self.serverError = nil
        } catch {
            self.serverError = error.localizedDescription
        }
    }

    public func refreshInterfaces(includeLoopback: Bool = false) {
        let ifaces = NetworkUtility.getActiveInterfaces(includeLoopback: includeLoopback)
        self.availableInterfaces = ifaces
        if let current = selectedInterface, !ifaces.contains(where: { $0.id == current.id }) {
            self.selectedInterface = ifaces.first
        } else if selectedInterface == nil {
            self.selectedInterface = ifaces.first
        }
    }

    public var currentHost: String {
        if isUsbMode {
            return "127.0.0.1"
        }
        return selectedInterface?.ipAddress ?? "127.0.0.1"
    }

    public var pairingString: String {
        credentials.qrCodeString(host: currentHost, port: Int(port))
    }

    public var adbReverseCommand: String {
        "adb reverse tcp:\(port) tcp:\(port)"
    }

    public func copyAdbCommand() {
        let pasteboard = NSPasteboard.general
        pasteboard.clearContents()
        pasteboard.setString(adbReverseCommand, forType: .string)
        adbCopied = true
        Task {
            try? await Task.sleep(nanoseconds: 2_000_000_000)
            self.adbCopied = false
        }
    }
}
