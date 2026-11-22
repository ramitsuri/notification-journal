package com.ramitsuri.notificationjournal.core.utils

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.io.encoding.Base64

class WorkCryptoCompatibilityTest {
    private val testPassword = "cross-platform-test-password"
    private val testSalt = "c2FsdDE2Ynl0ZXNhbHQ="

    private suspend fun createTestEncryptionHelper(): EncryptionHelper {
        val keyValueStore = getTestDataKeyValueStore()
        val helper = EncryptionHelper(keyValueStore)
        helper.setPassword(testPassword)
        helper.setSalt(testSalt)
        return helper
    }

    @Test
    fun testEncryptionRoundTripWithWorkCredentials() =
        runBlocking {
            val helper = createTestEncryptionHelper()
            val plaintext = "Hello from NotificationJournal work quick capture!"
            val encrypted = helper.encrypt(plaintext)
            val decrypted = helper.decrypt(encrypted)

            assertEquals(plaintext, decrypted)
        }

    @Test
    fun testDecryptSwiftCiphertext() =
        runBlocking {
            val helper = createTestEncryptionHelper()
            val swiftCiphertextBase64 = "C15dxe35pek4F6N0fYxtNBZazou0iPpGXjHrI7qqJlBhromuxAVEVHhzGXhcVZ/fwFv2WgZ8+w=="
            val cipherBytes = Base64.decode(swiftCiphertextBase64)
            val decrypted = helper.decrypt(cipherBytes)

            assertEquals("Hello from Swift to Kotlin!", decrypted)
        }

    @Test
    fun testLocalDateTimeParsingWithoutZ() {
        val dateString = "2026-11-21T21:15:00"
        val parsed = LocalDateTime.parse(dateString)

        assertEquals(2026, parsed.year)
        assertEquals(Month.NOVEMBER, parsed.month)
        assertEquals(21, parsed.day)
        assertEquals(21, parsed.hour)
        assertEquals(15, parsed.minute)
        assertEquals(0, parsed.second)
    }
}
