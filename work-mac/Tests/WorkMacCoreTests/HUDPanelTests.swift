import Testing
import AppKit
@testable import WorkMacCore

@MainActor
struct HUDPanelTests {

    @Test func panelHasCorrectWindowAttributes() {
        let panel = HUDPanel(contentRect: NSRect(x: 0, y: 0, width: 560, height: 140))

        #expect(panel.canBecomeKey == true)
        #expect(panel.canBecomeMain == false)
        #expect(panel.isFloatingPanel == true)
        #expect(panel.level == .floating)
        #expect(panel.collectionBehavior.contains(.canJoinAllSpaces))
        #expect(panel.collectionBehavior.contains(.fullScreenAuxiliary))
        #expect(panel.isOpaque == false)
        #expect(panel.backgroundColor == .clear)
    }

    @Test func positionCenteredCalculatesCenterPoint() {
        let panel = HUDPanel(contentRect: NSRect(x: 0, y: 0, width: 560, height: 100))
        if let mainScreen = NSScreen.main {
            panel.positionCentered(on: mainScreen)
            let frame = panel.frame
            let screenFrame = mainScreen.visibleFrame
            let expectedX = screenFrame.origin.x + (screenFrame.width - 560) / 2.0
            let expectedY = screenFrame.origin.y + (screenFrame.height - 100) / 2.0
            #expect(abs(frame.origin.x - expectedX) < 1.0)
            #expect(abs(frame.origin.y - expectedY) < 1.0)
        }
    }

    @Test func cancelOperationDismissesPanelAndCallsOnDismiss() {
        let panel = HUDPanel(contentRect: NSRect(x: 0, y: 0, width: 560, height: 140))
        var didDismiss = false
        panel.onDismiss = {
            didDismiss = true
        }

        panel.cancelOperation(nil)

        #expect(didDismiss == true)
        #expect(panel.isVisible == false)
    }

    @Test func hudControllerToggleAndLifecycle() {
        let vm = HUDViewModel(
            sendHandler: { _ in "ack" },
            isConnected: { true }
        )
        let controller = HUDController(viewModel: vm)

        #expect(controller.panel.isVisible == false)

        controller.present()
        #expect(controller.panel.canBecomeKey == true)

        controller.dismiss()
        #expect(controller.panel.isVisible == false)

        // Hotkey start and stop
        controller.startHotKey()
        controller.stopHotKey()
    }
}
