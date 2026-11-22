package com.ramitsuri.notificationjournal.core.utils

import com.ramitsuri.notificationjournal.core.model.WorkProfile

data class HomeQrCode(
    val salt: String,
    val password: String,
    val host: String,
)

object QrCodeParser {
    const val WORK_PREFIX = "work;;;"

    fun isWorkQr(qrCodeContent: String): Boolean = qrCodeContent.trim().startsWith(WORK_PREFIX)

    fun parseWorkQr(qrCodeContent: String): WorkProfile? {
        val trimmed = qrCodeContent.trim()
        if (!isWorkQr(trimmed)) {
            return null
        }
        val parts = trimmed.split(";;;")
        if (parts.size != 4) {
            return null
        }
        val salt = parts[1]
        val password = parts[2]
        val hostPart = parts[3]
        if (salt.isBlank() || password.isBlank() || hostPart.isBlank()) {
            return null
        }
        val (host, port) = WorkProfile.parseHostAndPort(hostPart)
        val profile =
            WorkProfile(
                host = host,
                port = port,
                salt = salt,
                password = password,
            )
        return if (profile.isValid()) profile else null
    }

    fun parseHomeQr(qrCodeContent: String): HomeQrCode? {
        val trimmed = qrCodeContent.trim()
        if (isWorkQr(trimmed)) {
            return null
        }
        val parts = trimmed.split(";;;")
        if (parts.size != 3) {
            return null
        }
        val salt = parts[0]
        val password = parts[1]
        val host = parts[2]
        if (salt.isBlank() || password.isBlank() || host.isBlank()) {
            return null
        }
        return HomeQrCode(
            salt = salt,
            password = password,
            host = host,
        )
    }
}
