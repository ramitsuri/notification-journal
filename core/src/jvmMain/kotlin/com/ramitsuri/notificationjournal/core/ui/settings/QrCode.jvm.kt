package com.ramitsuri.notificationjournal.core.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter

@Composable
actual fun QrCodeScanner(
    modifier: Modifier,
    onQrCodeScanned: (String) -> Unit,
) { }

@Composable
actual fun QrCodeImage(
    modifier: Modifier,
    content: String,
) {
    val size = 512
    val bitmap =
        remember(content) {
            val bitMatrix =
                MultiFormatWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    size,
                    size,
                    null,
                )
            MatrixToImageWriter.toBufferedImage(bitMatrix)
        }
    Image(
        modifier = modifier,
        bitmap = bitmap.toComposeImageBitmap(),
        contentDescription = null,
    )
}
