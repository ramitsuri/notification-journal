import Foundation

public struct NetworkInterface: Identifiable, Hashable, Sendable {
    public var id: String { "\(name)-\(ipAddress)" }
    public let name: String
    public let ipAddress: String
    public let isLoopback: Bool

    public init(name: String, ipAddress: String, isLoopback: Bool) {
        self.name = name
        self.ipAddress = ipAddress
        self.isLoopback = isLoopback
    }
}

public enum NetworkUtility {
    public static var loopbackInterface: NetworkInterface {
        NetworkInterface(name: "lo0", ipAddress: "127.0.0.1", isLoopback: true)
    }

    public static func getActiveInterfaces(includeLoopback: Bool = false) -> [NetworkInterface] {
        var interfaces = [NetworkInterface]()
        var ifaddrPointer: UnsafeMutablePointer<ifaddrs>?

        guard getifaddrs(&ifaddrPointer) == 0, let firstAddr = ifaddrPointer else {
            if includeLoopback {
                return [loopbackInterface]
            }
            return []
        }
        defer { freeifaddrs(ifaddrPointer) }

        var cursor: UnsafeMutablePointer<ifaddrs>? = firstAddr
        var seen = Set<String>()

        while let current = cursor {
            let ifa = current.pointee
            let flags = Int32(ifa.ifa_flags)
            let isUp = (flags & IFF_UP) != 0
            let isRunning = (flags & IFF_RUNNING) != 0
            let isLoopback = (flags & IFF_LOOPBACK) != 0

            if isUp && isRunning, let addr = ifa.ifa_addr, addr.pointee.sa_family == UInt8(AF_INET) {
                let name = String(cString: ifa.ifa_name)

                // Filter for standard LAN interfaces (en0, en1, etc.) or loopback when requested
                let isLanInterface = name.hasPrefix("en")

                if isLoopback {
                    if includeLoopback {
                        var sinAddr = addr.withMemoryRebound(to: sockaddr_in.self, capacity: 1) { $0.pointee.sin_addr }
                        var buffer = [CChar](repeating: 0, count: Int(INET_ADDRSTRLEN))
                        if inet_ntop(AF_INET, &sinAddr, &buffer, socklen_t(INET_ADDRSTRLEN)) != nil {
                            let ip = buffer.withUnsafeBufferPointer { ptr -> String in
                                guard let base = ptr.baseAddress else { return "" }
                                return String(cString: base)
                            }
                            let key = "\(name):\(ip)"
                            if !ip.isEmpty && ip != "0.0.0.0" && !seen.contains(key) {
                                seen.insert(key)
                                interfaces.append(NetworkInterface(name: name, ipAddress: ip, isLoopback: true))
                            }
                        }
                    }
                } else if isLanInterface {
                    var sinAddr = addr.withMemoryRebound(to: sockaddr_in.self, capacity: 1) { $0.pointee.sin_addr }
                    var buffer = [CChar](repeating: 0, count: Int(INET_ADDRSTRLEN))
                    if inet_ntop(AF_INET, &sinAddr, &buffer, socklen_t(INET_ADDRSTRLEN)) != nil {
                        let ip = buffer.withUnsafeBufferPointer { ptr -> String in
                            guard let base = ptr.baseAddress else { return "" }
                            return String(cString: base)
                        }
                        let key = "\(name):\(ip)"
                        if !ip.isEmpty && ip != "0.0.0.0" && !seen.contains(key) {
                            seen.insert(key)
                            interfaces.append(NetworkInterface(name: name, ipAddress: ip, isLoopback: false))
                        }
                    }
                }
            }
            cursor = ifa.ifa_next
        }

        // Sort: en0 first, then en1, then remaining en*
        interfaces.sort { lhs, rhs in
            if lhs.name == "en0" { return true }
            if rhs.name == "en0" { return false }
            if lhs.name == "en1" { return true }
            if rhs.name == "en1" { return false }
            return lhs.name < rhs.name
        }

        return interfaces
    }
}
