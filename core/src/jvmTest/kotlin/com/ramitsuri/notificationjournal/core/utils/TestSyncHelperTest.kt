package com.ramitsuri.notificationjournal.core.utils

import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.nio.file.Paths
import kotlin.io.path.deleteRecursively

class TestSyncHelperTest {
    private lateinit var keyValueStore: KeyValueStoreV2
    private lateinit var encryptionHelper: EncryptionHelper
    private lateinit var prefManager: PrefManager

    @Before
    fun setUp() {
        keyValueStore = getTestDataKeyValueStore()
        encryptionHelper = EncryptionHelper(keyValueStore)
        prefManager = PrefManager(testJson, keyValueStore)
    }

    @After
    fun tearDown() {
        Paths.get(Constants.BASE_DIR).deleteRecursively()
    }

    @Test
    fun applyDummySyncConfig_desktopSetsMatchingSecretAndLocalhost() =
        runTest {
            TestSyncHelper.applyDummySyncConfig(
                encryptionHelper = encryptionHelper,
                prefManager = prefManager,
                isDesktop = true,
            )

            assertEquals(TestSyncHelper.DUMMY_ENCRYPTION_PASSWORD, encryptionHelper.getPassword().first())
            assertEquals(TestSyncHelper.DUMMY_ENCRYPTION_SALT, encryptionHelper.getSalt().first())

            val props = prefManager.getDataHostProperties().first()
            assertEquals("Desktop", props.deviceName)
            assertEquals("desktop-test-device", props.deviceId)
            assertEquals("127.0.0.1", props.dataHost)
        }

    @Test
    fun applyDummySyncConfig_androidSetsMatchingSecretAndEmulatorHost() =
        runTest {
            TestSyncHelper.applyDummySyncConfig(
                encryptionHelper = encryptionHelper,
                prefManager = prefManager,
                isDesktop = false,
            )

            assertEquals(TestSyncHelper.DUMMY_ENCRYPTION_PASSWORD, encryptionHelper.getPassword().first())
            assertEquals(TestSyncHelper.DUMMY_ENCRYPTION_SALT, encryptionHelper.getSalt().first())

            val props = prefManager.getDataHostProperties().first()
            assertEquals("Android", props.deviceName)
            assertEquals("android-test-device", props.deviceId)
            assertEquals("10.0.2.2", props.dataHost)
        }

    @Test
    fun applyDummySyncConfig_preservesCustomHostUnlessOverridden() =
        runTest {
            prefManager.setDataHostProperties(
                DataHostProperties(
                    deviceName = "MyCustomDevice",
                    deviceId = "my-custom-id",
                    dataHost = "192.168.1.150",
                ),
            )

            TestSyncHelper.applyDummySyncConfig(
                encryptionHelper = encryptionHelper,
                prefManager = prefManager,
                isDesktop = false,
                overrideHost = false,
            )

            val props = prefManager.getDataHostProperties().first()
            assertEquals("MyCustomDevice", props.deviceName)
            assertEquals("my-custom-id", props.deviceId)
            assertEquals("192.168.1.150", props.dataHost)

            // When overrideHost = true, it resets
            TestSyncHelper.applyDummySyncConfig(
                encryptionHelper = encryptionHelper,
                prefManager = prefManager,
                isDesktop = false,
                overrideHost = true,
            )

            val overriddenProps = prefManager.getDataHostProperties().first()
            assertEquals("10.0.2.2", overriddenProps.dataHost)
        }
}
