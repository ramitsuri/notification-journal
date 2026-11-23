import AppKit
import SwiftUI
import WorkMacCore

@main
struct WorkMacApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        Settings {
            EmptyView()
        }
    }
}

@MainActor
final class AppDelegate: NSObject, NSApplicationDelegate {
    let appState = WorkAppState()
    private var menuBarController: WorkMenuBarController?

    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApp.setActivationPolicy(.accessory)
        let controller = WorkMenuBarController(appState: appState)
        controller.startHotKey()
        self.menuBarController = controller
    }

    func applicationWillTerminate(_ notification: Notification) {
        menuBarController?.stopHotKey()
    }
}
