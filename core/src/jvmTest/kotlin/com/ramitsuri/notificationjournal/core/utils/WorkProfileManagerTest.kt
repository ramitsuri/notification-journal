package com.ramitsuri.notificationjournal.core.utils

import app.cash.turbine.test
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.model.WorkProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkProfileManagerTest : BaseTest() {
    private lateinit var keyValueStore: KeyValueStoreV2
    private lateinit var workProfileManager: WorkProfileManager

    @Before
    fun setUp() {
        keyValueStore = getTestDataKeyValueStore()
        workProfileManager = WorkProfileManager(keyValueStore)
    }

    @Test
    fun getProfile_whenNoProfileSaved_returnsNull() =
        runTest {
            assertNull(workProfileManager.getProfile())
            assertFalse(workProfileManager.hasProfile())
        }

    @Test
    fun setProfile_persistsAndRetrievesPropertiesWithExplicitPort() =
        runTest {
            val profile =
                WorkProfile(
                    host = "192.168.1.100",
                    port = 9090,
                    salt = "test-salt",
                    password = "test-password",
                )

            workProfileManager.setProfile(profile)

            val retrieved = workProfileManager.getProfile()
            assertNotNull(retrieved)
            assertEquals("192.168.1.100", retrieved!!.host)
            assertEquals(9090, retrieved.port)
            assertEquals("test-salt", retrieved.salt)
            assertEquals("test-password", retrieved.password)
            assertTrue(workProfileManager.hasProfile())
        }

    @Test
    fun getProfile_whenPortIsNonPositive_returnsNull() =
        runTest {
            keyValueStore.putString(Key.WORK_HOST, "192.168.1.100:0")
            keyValueStore.putString(Key.WORK_SALT, "salt")
            keyValueStore.putString(Key.WORK_PASSWORD, "password")

            assertNull(workProfileManager.getProfile())
            assertFalse(workProfileManager.hasProfile())
        }

    @Test
    fun setProfile_whenHostAlreadyContainsPort_handlesWithoutDuplication() =
        runTest {
            val profile =
                WorkProfile(
                    host = "192.168.1.50:9090",
                    port = 9090,
                    salt = "salt",
                    password = "password",
                )

            workProfileManager.setProfile(profile)

            val retrieved = workProfileManager.getProfile()
            assertNotNull(retrieved)
            assertEquals("192.168.1.50", retrieved!!.host)
            assertEquals(9090, retrieved.port)
        }

    @Test
    fun getProfile_defaultsPortTo8080WhenStoredWithoutPort() =
        runTest {
            keyValueStore.putString(Key.WORK_HOST, "192.168.1.200")
            keyValueStore.putString(Key.WORK_SALT, "salt")
            keyValueStore.putString(Key.WORK_PASSWORD, "password")

            val retrieved = workProfileManager.getProfile()
            assertNotNull(retrieved)
            assertEquals("192.168.1.200", retrieved!!.host)
            assertEquals(8080, retrieved.port)
        }

    @Test
    fun clearProfile_removesSavedWorkCredentials() =
        runTest {
            workProfileManager.setProfile(
                WorkProfile(
                    host = "192.168.1.100",
                    port = 8080,
                    salt = "salt",
                    password = "password",
                ),
            )
            assertTrue(workProfileManager.hasProfile())

            workProfileManager.clearProfile()

            assertNull(workProfileManager.getProfile())
            assertFalse(workProfileManager.hasProfile())
        }

    @Test
    fun getProfileFlow_emitsUpdates() =
        runTest {
            workProfileManager.getProfileFlow().test {
                assertNull(awaitItem())

                workProfileManager.setProfile(
                    WorkProfile(
                        host = "192.168.1.50",
                        port = 8080,
                        salt = "salt",
                        password = "password",
                    ),
                )

                val updated = awaitItem()
                assertNotNull(updated)
                assertEquals("192.168.1.50", updated!!.host)
                assertEquals(8080, updated.port)

                workProfileManager.clearProfile()
                assertNull(awaitItem())
            }
        }

    @Test
    fun workProfileOperations_doNotMutateHomeSyncSettings() =
        runTest {
            val homeHostProps =
                DataHostProperties(
                    deviceName = "Home Device",
                    deviceId = "home-id-123",
                    dataHost = "192.168.1.1:8000",
                )
            prefManager.setDataHostProperties(homeHostProps)
            encryptionHelper.setPassword("home-password")
            encryptionHelper.setSalt("home-salt")

            val workProfile =
                WorkProfile(
                    host = "10.0.0.99",
                    port = 8080,
                    salt = "work-salt",
                    password = "work-password",
                )
            workProfileManager.setProfile(workProfile)

            // Verify work profile is saved
            val retrievedWork = workProfileManager.getProfile()
            assertEquals("10.0.0.99", retrievedWork!!.host)
            assertEquals("work-salt", retrievedWork.salt)
            assertEquals("work-password", retrievedWork.password)

            // Verify home settings remain completely untouched
            val currentHomeHostProps = prefManager.getDataHostProperties().first()
            assertEquals("192.168.1.1:8000", currentHomeHostProps.dataHost)
            assertEquals("Home Device", currentHomeHostProps.deviceName)
            assertEquals("home-id-123", currentHomeHostProps.deviceId)
            assertEquals("home-password", encryptionHelper.getPassword().first())
            assertEquals("home-salt", encryptionHelper.getSalt().first())
        }
}
