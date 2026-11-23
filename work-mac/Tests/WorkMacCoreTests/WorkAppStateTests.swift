import Testing
import Foundation
@testable import WorkMacCore

@MainActor
struct WorkAppStateTests {
    @Test func initialStateHasCredentialsAndDefaultPort() {
        let creds = SessionCredentials(password: "test-pw", salt: "c2FsdDE2Ynl0ZXNhbHQ=")
        let iface = NetworkInterface(name: "en0", ipAddress: "192.168.1.100", isLoopback: false)
        let state = WorkAppState(credentials: creds, interfaces: [iface], startServerImmediately: false)

        #expect(state.credentials == creds)
        #expect(state.port == 8080)
        #expect(state.isClientConnected == false)
        #expect(state.isUsbMode == false)
        #expect(state.selectedInterface?.ipAddress == "192.168.1.100")
        #expect(state.currentHost == "192.168.1.100")
        #expect(state.pairingString == "work;;;c2FsdDE2Ynl0ZXNhbHQ=;;;test-pw;;;192.168.1.100:8080")
        #expect(state.adbReverseCommand == "adb reverse tcp:8080 tcp:8080")
    }

    @Test func switchingToUsbModeChangesHostToLoopback() {
        let creds = SessionCredentials(password: "test-pw", salt: "c2FsdDE2Ynl0ZXNhbHQ=")
        let iface = NetworkInterface(name: "en0", ipAddress: "192.168.1.100", isLoopback: false)
        let state = WorkAppState(credentials: creds, interfaces: [iface], startServerImmediately: false)

        state.isUsbMode = true
        #expect(state.currentHost == "127.0.0.1")
        #expect(state.pairingString == "work;;;c2FsdDE2Ynl0ZXNhbHQ=;;;test-pw;;;127.0.0.1:8080")

        state.isUsbMode = false
        #expect(state.currentHost == "192.168.1.100")
        #expect(state.pairingString == "work;;;c2FsdDE2Ynl0ZXNhbHQ=;;;test-pw;;;192.168.1.100:8080")
    }

    @Test func selectingDifferentInterfaceUpdatesPairingString() {
        let creds = SessionCredentials(password: "test-pw", salt: "c2FsdDE2Ynl0ZXNhbHQ=")
        let en0 = NetworkInterface(name: "en0", ipAddress: "192.168.1.100", isLoopback: false)
        let en1 = NetworkInterface(name: "en1", ipAddress: "10.0.0.25", isLoopback: false)
        let state = WorkAppState(credentials: creds, interfaces: [en0, en1], startServerImmediately: false)

        #expect(state.currentHost == "192.168.1.100")

        state.selectedInterface = en1
        #expect(state.currentHost == "10.0.0.25")
        #expect(state.pairingString == "work;;;c2FsdDE2Ynl0ZXNhbHQ=;;;test-pw;;;10.0.0.25:8080")
    }

    @Test func rotateSecretsGeneratesNewCredentialsAndUpdatesQR() async {
        let creds = SessionCredentials(password: "initial-pw", salt: "c2FsdDE2Ynl0ZXNhbHQ=")
        let iface = NetworkInterface(name: "en0", ipAddress: "192.168.1.100", isLoopback: false)
        let state = WorkAppState(credentials: creds, interfaces: [iface], startServerImmediately: false)

        #expect(state.pairingString == "work;;;c2FsdDE2Ynl0ZXNhbHQ=;;;initial-pw;;;192.168.1.100:8080")

        await state.rotateSecrets()

        #expect(state.credentials.password != "initial-pw")
        #expect(state.credentials.salt != "c2FsdDE2Ynl0ZXNhbHQ=")
        #expect(state.pairingString != "work;;;c2FsdDE2Ynl0ZXNhbHQ=;;;initial-pw;;;192.168.1.100:8080")
        #expect(state.pairingString.hasPrefix("work;;;"))
        #expect(state.pairingString.contains("192.168.1.100:8080"))
    }

    @Test func fallbackToLoopbackWhenNoInterfacesAvailable() {
        let creds = SessionCredentials(password: "pw", salt: "c2FsdDE2Ynl0ZXNhbHQ=")
        let state = WorkAppState(credentials: creds, interfaces: [], startServerImmediately: false)

        #expect(state.selectedInterface == nil)
        #expect(state.currentHost == "127.0.0.1")
        #expect(state.pairingString == "work;;;c2FsdDE2Ynl0ZXNhbHQ=;;;pw;;;127.0.0.1:8080")
    }

    @Test func zeroDiskPersistenceGuaranteed() {
        let creds = SessionCredentials(password: "pw-check", salt: "c2FsdDE2Ynl0ZXNhbHQ=")
        _ = WorkAppState(credentials: creds, startServerImmediately: false)

        let defaults = UserDefaults.standard
        #expect(defaults.string(forKey: "work_password") == nil)
        #expect(defaults.string(forKey: "work_salt") == nil)
        #expect(defaults.string(forKey: "work_host") == nil)
        #expect(defaults.string(forKey: "work_entries") == nil)
    }
}
