import Foundation
import Security

public struct SessionCredentials: Equatable, Sendable {
    public let password: String
    public let salt: String

    public init() {
        self.password = UUID().uuidString.lowercased()

        var randomBytes = [UInt8](repeating: 0, count: 16)
        let status = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
        if status == errSecSuccess {
            self.salt = Data(randomBytes).base64EncodedString()
        } else {
            // Fallback to UUID-based salt if SecRandomCopyBytes fails
            self.salt = Data(UUID().uuidString.utf8.prefix(16)).base64EncodedString()
        }
    }

    public init(password: String, salt: String) {
        self.password = password
        self.salt = salt
    }

    public func qrCodeString(host: String, port: Int) -> String {
        "work;;;\(salt);;;\(password);;;\(host):\(port)"
    }
}
