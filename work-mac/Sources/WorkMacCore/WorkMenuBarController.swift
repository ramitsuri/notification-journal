import AppKit
import Combine
import SwiftUI

@MainActor
public final class WorkMenuBarController: NSObject, NSPopoverDelegate {
    public let appState: WorkAppState
    public let statusItem: NSStatusItem
    public let popover: NSPopover
    private var hotKeyManager: CarbonHotKeyManager?
    private var cancellables = Set<AnyCancellable>()
    private var lastCloseTime: TimeInterval = 0

    public init(appState: WorkAppState, statusItem: NSStatusItem? = nil) {
        self.appState = appState
        self.statusItem = statusItem ?? NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
        let pop = NSPopover()
        self.popover = pop
        super.init()

        setupStatusItem()
        setupPopover()
        setupConnectionObserver()
    }

    private func setupStatusItem() {
        if let button = statusItem.button {
            updateStatusImage(isConnected: appState.isClientConnected)
            button.target = self
            button.action = #selector(statusItemClicked)
        }
    }

    private func setupPopover() {
        popover.behavior = .transient
        popover.animates = true
        let popoverView = PopoverView(appState: appState)
        let hostingController = NSHostingController(rootView: popoverView)
        popover.contentViewController = hostingController
        popover.delegate = self

        appState.hudViewModel.setDismissHandler { [weak self] in
            self?.closePopover()
        }
    }

    private func setupConnectionObserver() {
        appState.$isClientConnected
            .receive(on: DispatchQueue.main)
            .sink { [weak self] isConnected in
                self?.updateStatusImage(isConnected: isConnected)
            }
            .store(in: &cancellables)
    }

    private func updateStatusImage(isConnected: Bool) {
        statusItem.button?.image = NSImage(
            systemSymbolName: isConnected ? "link.circle.fill" : "link.circle",
            accessibilityDescription: "NotificationJournal Work"
        )
    }

    @objc private func statusItemClicked() {
        let now = ProcessInfo.processInfo.systemUptime
        if now - lastCloseTime < 0.2 {
            return
        }
        togglePopover()
    }

    public func popoverDidClose(_ notification: Notification) {
        lastCloseTime = ProcessInfo.processInfo.systemUptime
    }

    public func startHotKey() {
        guard hotKeyManager == nil else { return }
        let manager = CarbonHotKeyManager { [weak self] in
            Task { @MainActor [weak self] in
                self?.togglePopover()
            }
        }
        _ = manager.register()
        self.hotKeyManager = manager
    }

    public func stopHotKey() {
        hotKeyManager?.unregister()
        hotKeyManager = nil
    }

    public func togglePopover() {
        if popover.isShown {
            closePopover()
        } else {
            showPopover()
        }
    }

    public func showPopover() {
        guard let button = statusItem.button else { return }
        if !popover.isShown {
            NSApp.activate(ignoringOtherApps: true)
            popover.show(relativeTo: button.bounds, of: button, preferredEdge: .minY)
            if let window = popover.contentViewController?.view.window {
                window.makeKey()
            }
        }
        appState.hudViewModel.refocus()
    }

    public func closePopover() {
        if popover.isShown {
            popover.performClose(nil)
        }
    }
}
