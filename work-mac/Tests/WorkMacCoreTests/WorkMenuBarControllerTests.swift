import AppKit
import Testing
@testable import WorkMacCore

@MainActor
struct WorkMenuBarControllerTests {

    @Test func controllerInitializesStatusItemAndPopover() {
        let state = WorkAppState(startServerImmediately: false)
        let controller = WorkMenuBarController(appState: state)

        #expect(controller.statusItem.button != nil)
        #expect(controller.statusItem.button?.target === controller)
        #expect(controller.popover.behavior == .transient)
        #expect(controller.popover.contentViewController != nil)
        #expect(controller.popover.animates == true)
    }

    @Test func popoverDismissHandlerClosesPopover() {
        let state = WorkAppState(startServerImmediately: false)
        let controller = WorkMenuBarController(appState: state)

        controller.showPopover()
        state.hudViewModel.dismiss()
        #expect(controller.popover.isShown == false)
    }

    @Test func hotKeyStartAndStop() {
        let state = WorkAppState(startServerImmediately: false)
        let controller = WorkMenuBarController(appState: state)

        controller.startHotKey()
        controller.stopHotKey()
    }

    @Test func statusImageNameMatchesConnection() {
        let state = WorkAppState(startServerImmediately: false)
        #expect(state.statusImageName == "link.circle")

        state.setClientConnected(true)
        #expect(state.statusImageName == "link.circle.fill")

        state.setClientConnected(false)
        #expect(state.statusImageName == "link.circle")
    }
}
