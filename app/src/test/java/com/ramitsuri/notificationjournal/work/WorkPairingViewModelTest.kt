package com.ramitsuri.notificationjournal.work

import com.ramitsuri.notificationjournal.core.utils.WorkProfileManager
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WorkPairingViewModelTest {
    private lateinit var keyValueStore: FakeKeyValueStore
    private lateinit var workProfileManager: WorkProfileManager
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: WorkPairingViewModel

    @Before
    fun setUp() {
        keyValueStore = FakeKeyValueStore()
        workProfileManager = WorkProfileManager(keyValueStore)
        viewModel = WorkPairingViewModel(workProfileManager, ioDispatcher = testDispatcher)
    }

    @Test
    fun onQrCodeScanned_rejectsNonWorkQrCode() =
        runTest(testDispatcher) {
            val handled = viewModel.onQrCodeScanned("salt;;;password;;;192.168.1.100:8000")
            assertFalse(handled)
            assertNull(workProfileManager.getProfile())
            assertFalse(viewModel.isPaired.value)
        }

    @Test
    fun onQrCodeScanned_rejectsMalformedWorkQrCode() =
        runTest(testDispatcher) {
            val handled = viewModel.onQrCodeScanned("work;;;salt;;;")
            assertFalse(handled)
            assertNull(workProfileManager.getProfile())
            assertFalse(viewModel.isPaired.value)
        }

    @Test
    fun onQrCodeScanned_persistsWorkProfileAndEmitsSuccess() =
        runTest(testDispatcher) {
            var emitted = false
            val job =
                launch {
                    viewModel.isPaired.filter { it }.first()
                    emitted = true
                }

            val validQr = "work;;;dGVzdC1zYWx0;;;test-secret;;;192.168.1.50:8080"
            val handled = viewModel.onQrCodeScanned(validQr)
            assertTrue(handled)

            val savedProfile = workProfileManager.getProfile()
            assertNotNull(savedProfile)
            assertEquals("192.168.1.50", savedProfile?.host)
            assertEquals(8080, savedProfile?.port)
            assertEquals("dGVzdC1zYWx0", savedProfile?.salt)
            assertEquals("test-secret", savedProfile?.password)
            assertTrue(emitted)
            assertTrue(viewModel.isPaired.value)

            job.cancel()
        }

    @Test
    fun onQrCodeScanned_ignoresSubsequentScansAfterSuccess() =
        runTest(testDispatcher) {
            val validQr = "work;;;salt1;;;pass1;;;192.168.1.10:8080"
            val firstHandled = viewModel.onQrCodeScanned(validQr)
            assertTrue(firstHandled)

            val secondQr = "work;;;salt2;;;pass2;;;192.168.1.20:8080"
            val secondHandled = viewModel.onQrCodeScanned(secondQr)
            assertFalse(secondHandled)

            val saved = workProfileManager.getProfile()
            assertEquals("192.168.1.10", saved?.host)
        }
}
