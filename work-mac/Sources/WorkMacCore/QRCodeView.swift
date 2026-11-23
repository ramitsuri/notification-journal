import AppKit
import CoreImage
import SwiftUI

public struct QRCodeView: View {
    public let text: String
    public var size: CGFloat

    public init(text: String, size: CGFloat = 180) {
        self.text = text
        self.size = size
    }

    public var body: some View {
        Group {
            if let cgImage = QRCodeGenerator.generateCGImage(from: text, scale: 8.0) {
                Image(decorative: cgImage, scale: 1.0)
                    .interpolation(.none)
                    .resizable()
                    .scaledToFit()
            } else {
                VStack(spacing: 4) {
                    Image(systemName: "qrcode")
                        .font(.largeTitle)
                        .foregroundColor(.secondary)
                    Text("QR Code Unavailable")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
        .frame(width: size, height: size)
        .padding(10)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color.gray.opacity(0.3), lineWidth: 1)
        )
    }
}
