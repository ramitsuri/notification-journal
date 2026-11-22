import CommonCrypto
import CryptoKit
import Foundation

public enum CryptoError: Error, Equatable, Sendable {
    case invalidSalt
    case keyDerivationFailed(status: Int32)
    case encryptionFailed
    case decryptionFailed
}

public final class CryptoEngine: Sendable {
    private let symmetricKey: SymmetricKey

    public init(credentials: SessionCredentials) throws {
        self.symmetricKey = try Self.deriveKey(
            password: credentials.password,
            saltBase64: credentials.salt
        )
    }

    public static func deriveKey(password: String, saltBase64: String) throws -> SymmetricKey {
        guard let saltData = Data(base64Encoded: saltBase64) else {
            throw CryptoError.invalidSalt
        }

        let passwordBytes = Array(password.utf8)
        let saltBytes = Array(saltData)
        var derivedKeyBytes = [UInt8](repeating: 0, count: 32) // 256 bits

        let result = CCKeyDerivationPBKDF(
            CCPBKDFAlgorithm(kCCPBKDF2),
            password,
            passwordBytes.count,
            saltBytes,
            saltBytes.count,
            CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256),
            600_000,
            &derivedKeyBytes,
            derivedKeyBytes.count
        )

        guard result == kCCSuccess else {
            throw CryptoError.keyDerivationFailed(status: result)
        }

        return SymmetricKey(data: derivedKeyBytes)
    }

    public func encrypt(plaintext: String) throws -> Data {
        let data = Data(plaintext.utf8)
        do {
            let sealedBox = try AES.GCM.seal(data, using: symmetricKey)
            guard let combined = sealedBox.combined else {
                throw CryptoError.encryptionFailed
            }
            return combined
        } catch {
            throw CryptoError.encryptionFailed
        }
    }

    public func decrypt(cipherData: Data) throws -> String {
        do {
            let sealedBox = try AES.GCM.SealedBox(combined: cipherData)
            let decryptedData = try AES.GCM.open(sealedBox, using: symmetricKey)
            guard let string = String(data: decryptedData, encoding: .utf8) else {
                throw CryptoError.decryptionFailed
            }
            return string
        } catch {
            throw CryptoError.decryptionFailed
        }
    }
}
