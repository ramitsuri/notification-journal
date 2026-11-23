import AppKit
import SwiftUI
import WorkMacCore

@main
struct WorkMacApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @StateObject private var appState = WorkAppState()

    var body: some Scene {
        MenuBarExtra("NotificationJournal Work", systemImage: appState.isClientConnected ? "link.circle.fill" : "link.circle") {
            PopoverView(appState: appState)
        }
        .menuBarExtraStyle(.window)
    }
}

final class AppDelegate: NSObject, NSApplicationDelegate {
    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApp.setActivationPolicy(.accessory)
    }
}
