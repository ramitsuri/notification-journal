package com.ramitsuri.notificationjournal.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeParserTest {
    @Test
    fun parseWorkQr_extractsAllFieldsWithExplicitPort() {
        val qrContent = "work;;;dGVzdC1zYWx0;;;secret-pass-123;;;192.168.1.50:9090"
        val profile = QrCodeParser.parseWorkQr(qrContent)

        assertNotNull(profile)
        assertEquals("192.168.1.50", profile!!.host)
        assertEquals(9090, profile.port)
        assertEquals("dGVzdC1zYWx0", profile.salt)
        assertEquals("secret-pass-123", profile.password)
    }

    @Test
    fun parseWorkQr_defaultsPortTo8080WhenPortNotSupplied() {
        val qrContent = "work;;;dGVzdC1zYWx0;;;secret-pass-123;;;192.168.1.50"
        val profile = QrCodeParser.parseWorkQr(qrContent)

        assertNotNull(profile)
        assertEquals("192.168.1.50", profile!!.host)
        assertEquals(8080, profile.port)
        assertEquals("dGVzdC1zYWx0", profile.salt)
        assertEquals("secret-pass-123", profile.password)
    }

    @Test
    fun parseWorkQr_rejectsCodeWithoutWorkPrefix() {
        // Mismatched: A home sync QR code scanned in work scanner
        val homeQrCode = "home-salt;;;home-pass;;;192.168.1.100:8000"
        assertFalse(QrCodeParser.isWorkQr(homeQrCode))
        assertNull(QrCodeParser.parseWorkQr(homeQrCode))
    }

    @Test
    fun parseWorkQr_rejectsMalformedOrTruncatedContent() {
        assertNull(QrCodeParser.parseWorkQr(""))
        assertNull(QrCodeParser.parseWorkQr("work;;;"))
        assertNull(QrCodeParser.parseWorkQr("work;;;salt;;;pass"))
        assertNull(QrCodeParser.parseWorkQr("work;;;salt;;;pass;;;"))
        assertNull(QrCodeParser.parseWorkQr("work;;;;;;pass;;;192.168.1.1"))
        assertNull(QrCodeParser.parseWorkQr("work;;;salt;;;;;;192.168.1.1"))
        assertNull(QrCodeParser.parseWorkQr("work;;;salt;;;pass;;;192.168.1.1:8080;;;extraneous"))
        assertNull(QrCodeParser.parseWorkQr("work;;;salt;;;pass;;;192.168.1.1:0"))
    }

    @Test
    fun parseHomeQr_extractsAllFields() {
        val qrContent = "home-salt;;;home-pass;;;192.168.1.100:8000"
        val homeQr = QrCodeParser.parseHomeQr(qrContent)

        assertNotNull(homeQr)
        assertEquals("home-salt", homeQr!!.salt)
        assertEquals("home-pass", homeQr.password)
        assertEquals("192.168.1.100:8000", homeQr.host)
    }

    @Test
    fun parseHomeQr_rejectsWorkQrCode() {
        // Cross-scan protection: A work QR code scanned in home settings scanner
        val workQrContent = "work;;;dGVzdC1zYWx0;;;secret-pass-123;;;192.168.1.50:8080"
        assertTrue(QrCodeParser.isWorkQr(workQrContent))
        assertNull(QrCodeParser.parseHomeQr(workQrContent))
    }

    @Test
    fun parseHomeQr_rejectsMalformedContent() {
        assertNull(QrCodeParser.parseHomeQr(""))
        assertNull(QrCodeParser.parseHomeQr("salt;;;pass"))
        assertNull(QrCodeParser.parseHomeQr(";;;pass;;;host"))
        assertNull(QrCodeParser.parseHomeQr("salt;;;;;;host"))
        assertNull(QrCodeParser.parseHomeQr("salt;;;pass;;;"))
        assertNull(QrCodeParser.parseHomeQr("salt;;;pass;;;host;;;extraneous"))
    }

    @Test
    fun parsers_handleLeadingAndTrailingWhitespace() {
        val workQr = "  work;;;salt;;;pass;;;10.0.0.1:8080 \n"
        val workProfile = QrCodeParser.parseWorkQr(workQr)
        assertNotNull(workProfile)
        assertEquals("10.0.0.1", workProfile!!.host)

        val homeQr = "\n salt;;;pass;;;10.0.0.1 \t"
        val homeProfile = QrCodeParser.parseHomeQr(homeQr)
        assertNotNull(homeProfile)
        assertEquals("10.0.0.1", homeProfile!!.host)
    }
}
