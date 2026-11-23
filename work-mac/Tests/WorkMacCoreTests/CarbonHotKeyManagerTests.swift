import Testing
import Foundation
@testable import WorkMacCore

struct CarbonHotKeyManagerTests {

    @Test func registerAndUnregisterLifecycle() {
        let manager = CarbonHotKeyManager(onTrigger: {})
        let registered = manager.register()
        #expect(registered == true)

        // Multiple registers are safe idempotent
        let secondRegister = manager.register()
        #expect(secondRegister == true)

        manager.unregister()
    }
}
