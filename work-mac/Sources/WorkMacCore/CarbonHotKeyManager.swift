import Carbon
import Foundation

public final class CarbonHotKeyManager: @unchecked Sendable {
    private var hotKeyRef: EventHotKeyRef?
    private var eventHandler: EventHandlerRef?
    private let onTrigger: @Sendable () -> Void

    public init(onTrigger: @escaping @Sendable () -> Void) {
        self.onTrigger = onTrigger
    }

    @discardableResult
    public func register() -> Bool {
        guard hotKeyRef == nil else { return true }

        var eventType = EventTypeSpec(
            eventClass: OSType(kEventClassKeyboard),
            eventKind: UInt32(kEventHotKeyPressed)
        )

        let selfPtr = Unmanaged.passUnretained(self).toOpaque()

        let installStatus = InstallEventHandler(
            GetEventDispatcherTarget(),
            { (_, event, userData) -> OSStatus in
                guard let userData = userData else { return noErr }
                let manager = Unmanaged<CarbonHotKeyManager>.fromOpaque(userData).takeUnretainedValue()
                manager.handleHotKey()
                return noErr
            },
            1,
            &eventType,
            selfPtr,
            &eventHandler
        )

        guard installStatus == noErr else {
            return false
        }

        // Hyper key: Cmd + Option + Ctrl + Shift
        let modifiers = UInt32(cmdKey | optionKey | controlKey | shiftKey)
        // Carbon virtual keycode for 'R' is 15 (kVK_ANSI_R)
        let keyCode: UInt32 = 15
        let hotKeyID = EventHotKeyID(signature: OSType(0x4E4A), id: 1) // 'NJ', 1

        let registerStatus = RegisterEventHotKey(
            keyCode,
            modifiers,
            hotKeyID,
            GetEventDispatcherTarget(),
            0,
            &hotKeyRef
        )

        if registerStatus != noErr {
            if let handler = eventHandler {
                RemoveEventHandler(handler)
                eventHandler = nil
            }
            return false
        }

        return true
    }

    public func unregister() {
        if let ref = hotKeyRef {
            UnregisterEventHotKey(ref)
            hotKeyRef = nil
        }
        if let handler = eventHandler {
            RemoveEventHandler(handler)
            eventHandler = nil
        }
    }

    private func handleHotKey() {
        onTrigger()
    }

    deinit {
        unregister()
    }
}
