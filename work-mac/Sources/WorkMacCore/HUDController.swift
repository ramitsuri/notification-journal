import AppKit
import SwiftUI

@MainActor
public final class HUDController {
    public let panel: HUDPanel
    public let viewModel: HUDViewModel
    private var hotKeyManager: CarbonHotKeyManager?

    public init(viewModel: HUDViewModel) {
        self.viewModel = viewModel
        let panel = HUDPanel()
        self.panel = panel

        let hostingController = NSHostingController(rootView: HUDView(viewModel: viewModel))
        panel.contentViewController = hostingController

        viewModel.setDismissHandler { [weak self] in
            self?.dismiss()
        }

        panel.onDismiss = {
            // Dismissed
        }
    }

    public func startHotKey() {
        guard hotKeyManager == nil else { return }
        let manager = CarbonHotKeyManager { [weak self] in
            Task { @MainActor [weak self] in
                self?.toggle()
            }
        }
        _ = manager.register()
        self.hotKeyManager = manager
    }

    public func stopHotKey() {
        hotKeyManager?.unregister()
        hotKeyManager = nil
    }

    public func toggle() {
        if panel.isVisible {
            dismiss()
        } else {
            present()
        }
    }

    public func present() {
        viewModel.refocus()
        panel.present()
    }

    public func dismiss() {
        panel.dismiss()
    }
}
