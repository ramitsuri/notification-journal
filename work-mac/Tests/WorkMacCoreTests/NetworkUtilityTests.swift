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

    @Test func tailscaleInterfaceIdentification() {
        let tsIface = NetworkInterface(name: "utun4", ipAddress: "100.74.156.101", isLoopback: false)
        #expect(tsIface.isTailscale == true)
        #expect(tsIface.displayName == "Tailscale")

        let tsBoundaryMin = NetworkInterface(name: "utun0", ipAddress: "100.64.0.1", isLoopback: false)
        #expect(tsBoundaryMin.isTailscale == true)

        let tsBoundaryMax = NetworkInterface(name: "utun0", ipAddress: "100.127.255.254", isLoopback: false)
        #expect(tsBoundaryMax.isTailscale == true)

        let outsideTsBelow = NetworkInterface(name: "utun0", ipAddress: "100.63.255.255", isLoopback: false)
        #expect(outsideTsBelow.isTailscale == false)
        #expect(outsideTsBelow.displayName == "utun0")

        let outsideTsAbove = NetworkInterface(name: "utun0", ipAddress: "100.128.0.1", isLoopback: false)
        #expect(outsideTsAbove.isTailscale == false)

        let standardLan = NetworkInterface(name: "en0", ipAddress: "192.168.1.50", isLoopback: false)
        #expect(standardLan.isTailscale == false)
        #expect(standardLan.displayName == "en0")
    }
}
