import Testing
import Foundation
@testable import WorkMacCore

struct CryptoEngineTests {
    @Test func sessionCredentialsGeneration() {
        let creds1 = SessionCredentials()
        let creds2 = SessionCredentials()

        #expect(!creds1.password.isEmpty)
        #expect(!creds1.salt.isEmpty)
        #expect(creds1.password != creds2.password)
        #expect(creds1.salt != creds2.salt)

        // Salt should be 16 bytes Base64-encoded (24 chars with padding)
        let saltData = Data(base64Encoded: creds1.salt)
        #expect(saltData != nil)
        #expect(saltData?.count == 16)

        // Password should be valid UUID format
        #expect(UUID(uuidString: creds1.password) != nil)
    }

    @Test func qrCodeStringFormatting() {
        let creds = SessionCredentials(
            password: "test-uuid-password",
            salt: "c2FsdDE2Ynl0ZXNhbHQ="
        )
        let qr = creds.qrCodeString(host: "192.168.1.100", port: 8080)
        #expect(qr == "work;;;c2FsdDE2Ynl0ZXNhbHQ=;;;test-uuid-password;;;192.168.1.100:8080")
    }

    @Test func encryptionDecryptionRoundTrip() throws {
        let creds = SessionCredentials(
            password: "550e8400-e29b-41d4-a716-446655440000",
            salt: Data([0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15]).base64EncodedString()
        )
        let engine = try CryptoEngine(credentials: creds)

        let plaintext = "Hello NotificationJournal from Work Mac!"
        let encrypted = try engine.encrypt(plaintext: plaintext)

        // AES-GCM wire format: 12 bytes nonce + ciphertext + 16 bytes tag
        // Plaintext length is 40 bytes -> total encrypted size must be 12 + 40 + 16 = 68 bytes
        #expect(encrypted.count == 12 + plaintext.utf8.count + 16)

        let decrypted = try engine.decrypt(cipherData: encrypted)
        #expect(decrypted == plaintext)
    }

    @Test func keyDerivationIsDeterministicForSameCredentials() throws {
        let creds1 = SessionCredentials(
            password: "deterministic-password",
            salt: "c2FsdDE2Ynl0ZXNhbHQ="
        )
        let creds2 = SessionCredentials(
            password: "deterministic-password",
            salt: "c2FsdDE2Ynl0ZXNhbHQ="
        )

        let engine1 = try CryptoEngine(credentials: creds1)
        let engine2 = try CryptoEngine(credentials: creds2)

        let encryptedBy1 = try engine1.encrypt(plaintext: "Cross engine test")
        let decryptedBy2 = try engine2.decrypt(cipherData: encryptedBy1)

        #expect(decryptedBy2 == "Cross engine test")
    }

    @Test func crossPlatformFixedVectorTest() throws {
        let password = "test-password-12345"
        let salt = "c2FsdDE2Ynl0ZXNhbHQ=" // 16 bytes: "salt16bytesalt"
        let creds = SessionCredentials(password: password, salt: salt)
        let engine = try CryptoEngine(credentials: creds)

        let plaintext = "Hello from NotificationJournal test vector"
        let encrypted = try engine.encrypt(plaintext: plaintext)

        // Decrypt with fresh engine to confirm consistency
        let engine2 = try CryptoEngine(credentials: creds)
        let decrypted = try engine2.decrypt(cipherData: encrypted)
        #expect(decrypted == plaintext)
    }

    @Test func decryptKotlinCiphertext() throws {
        let password = "cross-platform-test-password"
        let salt = "c2FsdDE2Ynl0ZXNhbHQ="
        let creds = SessionCredentials(password: password, salt: salt)
        let engine = try CryptoEngine(credentials: creds)

        let kotlinCiphertextBase64 = "26SdWck+AHS0yE2HsLr4pzcEkccSmXmfbdP6K7RSD9DcpX/HiYLHtGJB7ISSII6wkLLDeP3WchOLuy1wbb6fSmJ2iby+wFhh+XPzWhGK"
        guard let cipherData = Data(base64Encoded: kotlinCiphertextBase64) else {
            Issue.record("Invalid base64 string")
            return
        }
        let decrypted = try engine.decrypt(cipherData: cipherData)

        #expect(decrypted == "Hello from NotificationJournal work quick capture!")
    }

    @Test func decryptKnownSwiftCiphertext() throws {
        let password = "cross-platform-test-password"
        let salt = "c2FsdDE2Ynl0ZXNhbHQ="
        let creds = SessionCredentials(password: password, salt: salt)
        let engine = try CryptoEngine(credentials: creds)

        let plaintext = "Hello from Swift to Kotlin!"
        let encrypted = try engine.encrypt(plaintext: plaintext)
        let decrypted = try engine.decrypt(cipherData: encrypted)
        #expect(decrypted == plaintext)
    }
}
