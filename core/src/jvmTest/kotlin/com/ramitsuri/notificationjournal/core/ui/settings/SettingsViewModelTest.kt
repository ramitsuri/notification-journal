package com.ramitsuri.notificationjournal.core.ui.settings

import app.cash.turbine.test
import com.ramitsuri.notificationjournal.core.model.DataHostProperties
import com.ramitsuri.notificationjournal.core.utils.BaseTest
import junit.framework.TestCase.assertTrue
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
                    exchangeName = ExchangeName(""),
                    deviceName = DeviceName(""),
                    username = Username(""),
                    password = Password(""),
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
                    exchangeName = ExchangeName(""),
                    deviceName = DeviceName(""),
                    username = Username(""),
                    password = Password(""),
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
                    exchangeName = ExchangeName(""),
                    deviceName = DeviceName(""),
                    username = Username(""),
                    password = Password(""),
                )

                val afterChange = awaitItem()
                assertTrue(afterChange.otherHosts.isEmpty())
            }
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
            )
    }
}
