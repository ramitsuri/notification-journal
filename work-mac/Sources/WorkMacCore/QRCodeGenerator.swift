import CoreImage
import Foundation

public enum QRCodeGenerator {
    public static func generateCGImage(from string: String, scale: CGFloat = 8.0) -> CGImage? {
        guard let filter = CIFilter(name: "CIQRCodeGenerator") else { return nil }
        let data = Data(string.utf8)
        filter.setValue(data, forKey: "inputMessage")
        filter.setValue("M", forKey: "inputCorrectionLevel")

        guard let outputCIImage = filter.outputImage else { return nil }

        let transform = CGAffineTransform(scaleX: scale, y: scale)
        let scaledCIImage = outputCIImage.transformed(by: transform)

        let context = CIContext(options: [.useSoftwareRenderer: false])
        return context.createCGImage(scaledCIImage, from: scaledCIImage.extent)
    }
}
