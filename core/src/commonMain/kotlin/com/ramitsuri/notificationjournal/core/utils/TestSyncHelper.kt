package com.ramitsuri.notificationjournal.core.utils

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.BuildKonfig
import kotlinx.coroutines.flow.first

object TestSyncHelper {
    private const val TAG = "TestSyncHelper"

    const val DUMMY_ENCRYPTION_PASSWORD = "test-dummy-password"

    // Base64 encoding of "test-dummy-salt"
    const val DUMMY_ENCRYPTION_SALT = "dGVzdC1kdW1teS1zYWx0"

    const val DESKTOP_DATA_HOST = "127.0.0.1"
    const val ANDROID_EMULATOR_DATA_HOST = "10.0.2.2"

    suspend fun applyDummySyncConfig(
        encryptionHelper: EncryptionHelper,
        prefManager: PrefManager,
        isDesktop: Boolean,
        overrideHost: Boolean = false,
    ) {
        if (!BuildKonfig.IS_DEBUG) {
            return
        }
        Logger.withTag(TAG).i { "Applying dummy sync configuration for isDesktop=$isDesktop" }

        encryptionHelper.setPassword(DUMMY_ENCRYPTION_PASSWORD)
        encryptionHelper.setSalt(DUMMY_ENCRYPTION_SALT)

        val currentProps = prefManager.getDataHostProperties().first()
        val defaultHost = if (isDesktop) DESKTOP_DATA_HOST else ANDROID_EMULATOR_DATA_HOST
        val defaultDeviceName = if (isDesktop) "Desktop" else "Android"
        val defaultDeviceId = if (isDesktop) "desktop-test-device" else "android-test-device"

        val targetHost =
            if (overrideHost || currentProps.dataHost.isBlank() || currentProps.dataHost == "http://") {
                defaultHost
            } else {
                currentProps.dataHost
            }

        val targetDeviceName =
            if (currentProps.deviceName.isBlank()) {
                defaultDeviceName
            } else {
                currentProps.deviceName
            }

        val targetDeviceId =
            if (currentProps.deviceId.isBlank()) {
                defaultDeviceId
            } else {
                currentProps.deviceId
            }

        val newProps =
            currentProps.copy(
                deviceName = targetDeviceName,
                deviceId = targetDeviceId,
                dataHost = targetHost,
            )

        prefManager.setDataHostProperties(newProps)
        Logger.withTag(TAG).i {
            "Applied dummy sync config: host=${newProps.dataHost}, device=${newProps.deviceName}, id=${newProps.deviceId}"
        }
    }
}
