import Testing
import Foundation
@testable import WorkMacCore

struct NetworkUtilityTests {
    @Test func loopbackInterfaceIsAvailable() {
        let loopback = NetworkUtility.loopbackInterface
        #expect(loopback.ipAddress == "127.0.0.1")
        #expect(loopback.isLoopback == true)
    }

    @Test func activeInterfacesReturnsValidIPv4Addresses() {
        let interfaces = NetworkUtility.getActiveInterfaces(includeLoopback: true)
        #expect(!interfaces.isEmpty)

        for iface in interfaces {
            #expect(!iface.name.isEmpty)
            #expect(!iface.ipAddress.isEmpty)
            #expect(iface.ipAddress != "0.0.0.0")

            // Validate standard IPv4 4-octet format
            let parts = iface.ipAddress.split(separator: ".")
            #expect(parts.count == 4)
            for part in parts {
                let num = Int(part)
                #expect(num != nil)
                #expect(num! >= 0 && num! <= 255)
            }
        }
    }

    @Test func nonLoopbackInterfacesExcludesLoopback() {
        let nonLoopback = NetworkUtility.getActiveInterfaces(includeLoopback: false)
        for iface in nonLoopback {
            #expect(!iface.isLoopback)
            #expect(!iface.ipAddress.hasPrefix("127."))
        }
    }
}
