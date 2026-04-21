package com.ramitsuri.notificationjournal.core.utils

import co.touchlab.kermit.Logger
import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.operations.IvAuthenticatedCipher
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.io.encoding.Base64
import kotlin.uuid.Uuid

class EncryptionHelper(
    private val keyValueStore: KeyValueStoreV2,
) {
    private val provider = CryptographyProvider.Default
    private val initMutex = Mutex()
    private var cipher: IvAuthenticatedCipher? = null

    fun getPassword(): Flow<String?> {
        return keyValueStore
            .getStringFlow(Key.ENCRYPTION_PASSWORD, "")
            .map { value ->
                value.takeIf { !it.isNullOrEmpty() }
            }
    }

    suspend fun setPassword(password: String) {
        initMutex.withLock {
            keyValueStore.putString(Key.ENCRYPTION_PASSWORD, password)
            cipher = null
        }
    }

    fun getSalt(): Flow<String?> {
        return keyValueStore
            .getStringFlow(Key.ENCRYPTION_SALT, "")
            .map { value ->
                value.takeIf { !it.isNullOrEmpty() }
            }
    }

    suspend fun setSalt(salt: String) {
        initMutex.withLock {
            keyValueStore.putString(Key.ENCRYPTION_SALT, salt)
            cipher = null
        }
    }

    suspend fun resetValues(overwrite: Boolean = false) {
        val currentPassword = getPassword().first()
        if (currentPassword.isNullOrEmpty() || overwrite) {
            val password = Uuid.random().toString()
            setPassword(password)
        }

        val currentSalt = getSalt().first()
        if (currentSalt.isNullOrEmpty() || overwrite) {
            val salt = CryptographyRandom.nextBytes(16)
            setSalt(Base64.encode(salt))
        }
    }

    suspend fun encrypt(plainText: String): ByteArray {
        return getCipher().encrypt(plainText.encodeToByteArray())
    }

    suspend fun decrypt(cipherText: ByteArray): String {
        return try {
            getCipher().decrypt(cipherText).decodeToString()
        } catch (e: Exception) {
            Logger.e(TAG) {
                "Error decrypting: ${e.stackTraceToString()}"
            }
            throw e
        }
    }

    private suspend fun getCipher(): IvAuthenticatedCipher =
        initMutex.withLock {
            cipher?.let { return it }
            val password = getPassword().first()?.encodeToByteArray() ?: error("Encryption password not set")
            val salt = getSalt().first()?.let { Base64.decode(it) } ?: error("Encryption salt not set")

            val key =
                provider.get(PBKDF2).secretDerivation(
                    digest = SHA256,
                    iterations = 600_000,
                    outputSize = 256.bits,
                    salt = salt,
                ).deriveSecretToByteArray(password)

            return provider
                .get(AES.GCM)
                .keyDecoder()
                .decodeFromByteArray(AES.Key.Format.RAW, key)
                .cipher()
                .also {
                    cipher = it
                }
        }

    companion object {
        private const val TAG = "EncryptionHelper"
    }
}
