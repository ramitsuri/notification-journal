package com.ramitsuri.notificationjournal.core.ui.settings

import app.cash.turbine.test
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.utils.BaseTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest : BaseTest() {
    private lateinit var viewModel: SettingsViewModel

    @Test
    fun `setting new host name adds old to other hosts`() =
        runTest {
            // Initial value
            prefManager.setDataHostProperties(
                DataHostProperties(dataHost = "host-1"),
            )
            prefManager.getDataHostProperties().test {
                assertTrue(awaitItem().otherHosts.isEmpty())

                viewModel.setDataSharingProperties(
                    dataHost = DataHost("host-2"),
                    deviceName = DeviceName(""),
                )

                val afterChange = awaitItem()
                assertTrue(afterChange.otherHosts.size == 1)
                assertTrue(afterChange.otherHosts.contains("host-1"))
            }
        }

    @Test
    fun `setting same host name adds it to other hosts`() =
        runTest {
            // Initial value
            prefManager.setDataHostProperties(
                DataHostProperties(dataHost = "host-1"),
            )
            prefManager.getDataHostProperties().test {
                assertTrue(awaitItem().otherHosts.isEmpty())

                viewModel.setDataSharingProperties(
                    dataHost = DataHost("host-1"),
                    deviceName = DeviceName(""),
                )

                val afterChange = awaitItem()
                assertTrue(afterChange.otherHosts.size == 1)
                assertTrue(afterChange.otherHosts.contains("host-1"))
            }
        }

    @Test
    fun `setting host name doesn't add it to other hosts if no other hosts currently`() =
        runTest {
            prefManager.getDataHostProperties().test {
                assertTrue(awaitItem().otherHosts.isEmpty())

                viewModel.setDataSharingProperties(
                    dataHost = DataHost("host-1"),
                    deviceName = DeviceName(""),
                )

                val afterChange = awaitItem()
                assertTrue(afterChange.otherHosts.isEmpty())
            }
        }

    @Test
    fun `onQrCodeScanned with valid home qr updates sync credentials`() =
        runTest {
            encryptionHelper.getPassword().test {
                val homeQr = "home-salt;;;home-pass;;;192.168.1.100:8000"
                viewModel.onQrCodeScanned(homeQr)

                var password = awaitItem()
                if (password == null) {
                    password = awaitItem()
                }
                assertEquals("home-pass", password)
                assertEquals("home-salt", encryptionHelper.getSalt().first())
                assertEquals("192.168.1.100:8000", prefManager.getDataHostProperties().first().dataHost)
            }
        }

    @Test
    fun `onQrCodeScanned with work qr rejects and does not overwrite home credentials`() =
        runTest {
            prefManager.setDataHostProperties(DataHostProperties(dataHost = "http://original-home"))
            encryptionHelper.setSalt("original-salt")
            encryptionHelper.setPassword("original-password")

            val workQr = "work;;;work-salt;;;work-pass;;;192.168.1.50:8080"
            viewModel.onQrCodeScanned(workQr)
            testScheduler.advanceUntilIdle()

            assertEquals("http://original-home", prefManager.getDataHostProperties().first().dataHost)
            assertEquals("original-salt", encryptionHelper.getSalt().first())
            assertEquals("original-password", encryptionHelper.getPassword().first())
        }

    @Test
    fun `onQrCodeScanned with invalid qr rejects and preserves existing credentials`() =
        runTest {
            prefManager.setDataHostProperties(DataHostProperties(dataHost = "http://original-home"))
            encryptionHelper.setSalt("original-salt")
            encryptionHelper.setPassword("original-password")

            viewModel.onQrCodeScanned("invalid-content-without-delimiters")
            testScheduler.advanceUntilIdle()

            assertEquals("http://original-home", prefManager.getDataHostProperties().first().dataHost)
            assertEquals("original-salt", encryptionHelper.getSalt().first())
            assertEquals("original-password", encryptionHelper.getPassword().first())
        }

    @Before
    fun setUp() {
        viewModel =
            SettingsViewModel(
                repository = repository,
                prefManager = prefManager,
                getAppVersion = { "1.0" },
                journalEntryDao = db.journalEntryDao(),
                conflictDao = db.entryConflictDao(),
                enableExport = true,
                encryptionHelper = encryptionHelper,
                getIpAddress = { null },
                actsAsServer = false,
            )
    }
}
