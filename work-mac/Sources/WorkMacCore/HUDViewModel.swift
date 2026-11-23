import Foundation
import SwiftUI

public typealias EntrySendHandler = @Sendable (JournalEntry) async throws -> String
public typealias ConnectionCheckHandler = @MainActor () -> Bool
public typealias DismissHandler = @MainActor () -> Void

@MainActor
public final class HUDViewModel: ObservableObject {
    @Published public var inputText: String = ""
    @Published public private(set) var inFlightItems: [InFlightItem] = []
    @Published public private(set) var errorMessage: String?
    @Published public private(set) var shakeTrigger: Int = 0
    @Published public private(set) var shakeOffset: CGFloat = 0
    @Published public private(set) var focusTrigger: UUID = UUID()

    private let sendHandler: EntrySendHandler
    private let isConnected: ConnectionCheckHandler
    private let fadeDelay: TimeInterval
    private var dismissHandler: DismissHandler?

    public init(
        sendHandler: @escaping EntrySendHandler,
        isConnected: @escaping ConnectionCheckHandler,
        fadeDelay: TimeInterval = 2.5,
        dismissHandler: DismissHandler? = nil
    ) {
        self.sendHandler = sendHandler
        self.isConnected = isConnected
        self.fadeDelay = fadeDelay
        self.dismissHandler = dismissHandler
    }

    public convenience init(
        appState: WorkAppState,
        fadeDelay: TimeInterval = 2.5,
        dismissHandler: DismissHandler? = nil
    ) {
        let server = appState.server
        self.init(
            sendHandler: { entry in
                try await server.send(entry: entry)
            },
            isConnected: { [weak appState] in
                appState?.isClientConnected ?? false
            },
            fadeDelay: fadeDelay,
            dismissHandler: dismissHandler
        )
    }

    public func setDismissHandler(_ handler: DismissHandler?) {
        self.dismissHandler = handler
    }

    public func refocus() {
        focusTrigger = UUID()
    }

    public func submit() {
        let trimmed = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        guard ensureConnected() else { return }

        let noteText = trimmed
        inputText = ""

        let item = InFlightItem(text: noteText, status: .pending)
        inFlightItems.insert(item, at: 0)

        dispatchSend(item: item)
    }

    public func retry(id: UUID) {
        guard let index = inFlightItems.firstIndex(where: { $0.id == id }) else { return }
        guard ensureConnected() else { return }

        inFlightItems[index].status = .pending
        let item = inFlightItems[index]

        dispatchSend(item: item)
    }

    public func discard(id: UUID) {
        withAnimation(.easeInOut(duration: 0.2)) {
            inFlightItems.removeAll(where: { $0.id == id })
        }
    }

    public func dismiss() {
        dismissHandler?()
    }

    private func ensureConnected() -> Bool {
        guard isConnected() else {
            errorMessage = "Phone is not connected"
            triggerShake()
            return false
        }
        errorMessage = nil
        return true
    }

    public func triggerShake() {
        shakeTrigger += 1
        shakeOffset = 10
        Task { @MainActor [weak self] in
            try? await Task.sleep(nanoseconds: 80_000_000)
            self?.shakeOffset = -8
            try? await Task.sleep(nanoseconds: 80_000_000)
            self?.shakeOffset = 6
            try? await Task.sleep(nanoseconds: 80_000_000)
            self?.shakeOffset = 0
        }
    }

    private func dispatchSend(item: InFlightItem) {
        let entry = JournalEntry(text: item.text)
        let itemId = item.id
        let sendHandler = self.sendHandler
        let fadeDelay = self.fadeDelay

        Task { [weak self] in
            do {
                _ = try await sendHandler(entry)
                Task { @MainActor [weak self] in
                    self?.handleSendSuccess(id: itemId, fadeDelay: fadeDelay)
                }
            } catch {
                Task { @MainActor [weak self] in
                    self?.handleSendFailure(id: itemId, error: error)
                }
            }
        }
    }

    private func handleSendSuccess(id: UUID, fadeDelay: TimeInterval) {
        guard let index = inFlightItems.firstIndex(where: { $0.id == id }) else { return }
        withAnimation(.easeInOut(duration: 0.25)) {
            inFlightItems[index].status = .acknowledged
        }

        Task { @MainActor [weak self] in
            if fadeDelay > 0 {
                try? await Task.sleep(nanoseconds: UInt64(fadeDelay * 1_000_000_000))
            }
            withAnimation(.easeInOut(duration: 0.35)) {
                self?.removeAcknowledgedItem(id: id)
            }
        }
    }

    private func handleSendFailure(id: UUID, error: Error) {
        guard let index = inFlightItems.firstIndex(where: { $0.id == id }) else { return }
        withAnimation(.easeInOut(duration: 0.2)) {
            inFlightItems[index].status = .failed(error.localizedDescription)
        }
    }

    private func removeAcknowledgedItem(id: UUID) {
        inFlightItems.removeAll(where: { $0.id == id && $0.status == .acknowledged })
    }
}
