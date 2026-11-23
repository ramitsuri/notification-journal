import AppKit

public final class HUDPanel: NSPanel {
    public var onDismiss: (() -> Void)?

    public init(contentRect: NSRect = NSRect(x: 0, y: 0, width: 560, height: 100)) {
        super.init(
            contentRect: contentRect,
            styleMask: [.borderless, .nonactivatingPanel],
            backing: .buffered,
            defer: false
        )
        self.isFloatingPanel = true
        self.level = .floating
        self.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary]
        self.isOpaque = false
        self.backgroundColor = .clear
        self.hasShadow = true
        self.hidesOnDeactivate = false
    }

    public override var canBecomeKey: Bool {
        true
    }

    public override var canBecomeMain: Bool {
        false
    }

    public override func cancelOperation(_ sender: Any?) {
        dismiss()
    }

    public func dismiss() {
        orderOut(nil)
        onDismiss?()
    }

    public func positionCentered(on screen: NSScreen? = NSScreen.main) {
        guard let targetScreen = screen ?? NSScreen.main else { return }
        let screenFrame = targetScreen.visibleFrame
        let panelSize = self.frame.size
        let x = screenFrame.origin.x + (screenFrame.width - panelSize.width) / 2.0
        let y = screenFrame.origin.y + (screenFrame.height - panelSize.height) / 2.0
        self.setFrameOrigin(NSPoint(x: x, y: y))
    }

    public func present() {
        positionCentered(on: NSScreen.main)
        makeKeyAndOrderFront(nil)
    }
}
