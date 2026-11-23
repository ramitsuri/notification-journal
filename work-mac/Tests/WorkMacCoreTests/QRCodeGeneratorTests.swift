import Testing
import Foundation
import CoreImage
@testable import WorkMacCore

struct QRCodeGeneratorTests {
    @Test func generateCGImageProducesValidImage() {
        let text = "work;;;c2FsdDE2Ynl0ZXNhbHQ=;;;550e8400-e29b-41d4-a716-446655440000;;;192.168.1.50:8080"
        let cgImage = QRCodeGenerator.generateCGImage(from: text, scale: 8.0)
        #expect(cgImage != nil)
        #expect(cgImage!.width > 0)
        #expect(cgImage!.height > 0)
    }

    @Test func generateQRDecodesToOriginalString() {
        let text = "work;;;c2FsdDE2Ynl0ZXNhbHQ=;;;550e8400-e29b-41d4-a716-446655440000;;;192.168.1.50:8080"
        guard let cgImage = QRCodeGenerator.generateCGImage(from: text, scale: 10.0) else {
            Issue.record("Failed to generate CGImage")
            return
        }

        let ciImage = CIImage(cgImage: cgImage)
        let detector = CIDetector(
            ofType: CIDetectorTypeQRCode,
            context: nil,
            options: [CIDetectorAccuracy: CIDetectorAccuracyHigh]
        )
        #expect(detector != nil)

        let features = detector?.features(in: ciImage) as? [CIQRCodeFeature] ?? []
        #expect(features.count == 1)
        #expect(features.first?.messageString == text)
    }

    @Test func generateQRScaleControlsDimensions() {
        let text = "test-scale"
        let imageScale4 = QRCodeGenerator.generateCGImage(from: text, scale: 4.0)
        let imageScale8 = QRCodeGenerator.generateCGImage(from: text, scale: 8.0)

        #expect(imageScale4 != nil)
        #expect(imageScale8 != nil)
        #expect(imageScale8!.width == imageScale4!.width * 2)
        #expect(imageScale8!.height == imageScale4!.height * 2)
    }
}
