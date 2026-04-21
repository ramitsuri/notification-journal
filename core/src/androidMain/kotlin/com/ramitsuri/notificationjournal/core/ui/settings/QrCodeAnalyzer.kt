package com.ramitsuri.notificationjournal.core.ui.settings

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import co.touchlab.kermit.Logger
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class QrCodeAnalyzer(private val onQrCodeScanned: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient()

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.forEach { barcode ->
                    barcode.rawValue?.let { onQrCodeScanned(it) }
                }
            }
            .addOnFailureListener {
                Logger.i("QRScanner") { "Error scanning QR code: $it" }
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
