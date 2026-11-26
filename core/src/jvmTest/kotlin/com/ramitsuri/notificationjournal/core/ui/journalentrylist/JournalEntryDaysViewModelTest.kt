package com.ramitsuri.notificationjournal.core.ui.journalentrylist

import com.ramitsuri.notificationjournal.core.data.ReconcileCheckDao
import com.ramitsuri.notificationjournal.core.model.EntryConflict
import com.ramitsuri.notificationjournal.core.model.ReconcileCheck
import com.ramitsuri.notificationjournal.core.model.ReconcileCheckMatchType
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.model.sync.Entity
import com.ramitsuri.notificationjournal.core.model.sync.Payload
import com.ramitsuri.notificationjournal.core.network.DataReceiveHelper
import com.ramitsuri.notificationjournal.core.network.VerifyEntriesHelper
import com.ramitsuri.notificationjournal.core.network.WebSocketHelper
import com.ramitsuri.notificationjournal.core.sync.AppLifecycleCoordinator
import com.ramitsuri.notificationjournal.core.utils.BaseTest
import com.ramitsuri.notificationjournal.core.utils.TestClock
import com.ramitsuri.notificationjournal.core.utils.testJson
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class JournalEntryDaysViewModelTest : BaseTest() {
    private lateinit var reconcileCheckDao: ReconcileCheckDao
    private lateinit var httpClient: HttpClient

    // Monday Jan 6, 2025 and Tuesday Jan 7, 2025
    private val mondayDate = LocalDate(2025, 1, 6)
    private val tuesdayDate = LocalDate(2025, 1, 7)

    @Before
    fun setUp() {
        (clock as TestClock).now = Instant.parse("2025-01-07T12:00:00Z")
        reconcileCheckDao = db.reconcileCheckDao()
        httpClient = HttpClient()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        httpClient.close()
        db.close()
    }

    @Test
    fun `onReconcileAll with untagged entries and conflicts exposes hard blocker and does not reconcile`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val viewModel = createViewModel()
            val collectJob = collectState(viewModel)

            db.tagsDao().insertIfPossible(Tag(id = "tag-1", order = 1, value = "Health"))
            db.journalEntryDao().insert(
                listOf(
                    JournalEntry(
                        id = "mon-untagged",
                        entryTime = LocalDateTime(2025, 1, 6, 9, 0),
                        text = "Untagged Monday",
                        tag = Tag.NO_TAG.value,
                    ),
                    JournalEntry(
                        id = "tue-tagged",
                        entryTime = LocalDateTime(2025, 1, 7, 9, 0),
                        text = "Woke up at 7",
                        tag = "Health",
                    ),
                    JournalEntry(
                        id = "tue-untagged",
                        entryTime = LocalDateTime(2025, 1, 7, 10, 0),
                        text = "Untagged Tuesday",
                        tag = Tag.NO_TAG.value,
                    ),
                ),
            )
            db.entryConflictDao().insert(
                EntryConflict(
                    id = "conflict-1",
                    entryId = "tue-tagged",
                    entryTime = LocalDateTime(2025, 1, 7, 9, 0),
                    text = "Woke up at 8",
                    tag = "Health",
                    senderName = "Phone",
                ),
            )

            viewModel.state.first { state ->
                state.dateWithCountList.size == 2 &&
                    state.dateWithCountList.sumOf { it.untaggedCount } == 2 &&
                    state.dateWithCountList.sumOf { it.conflictCount } == 1
            }

            viewModel.onReconcileAll(upload = true)

            val blockerState = viewModel.state.first { it.reconcileAllDialogState != null }
            assertEquals(
                ReconcileAllDialogState.HardBlocker(untaggedCount = 2, conflictCount = 1),
                blockerState.reconcileAllDialogState,
            )
            assertFalse(db.journalEntryDao().get("mon-untagged")!!.reconciled)
            assertFalse(db.journalEntryDao().get("tue-tagged")!!.reconciled)
            assertEquals(0, dataSendHelper.sendEntriesCallCount)

            viewModel.dismissReconcileAllDialog()
            val dismissedState = viewModel.state.first { it.reconcileAllDialogState == null }
            assertNull(dismissedState.reconcileAllDialogState)
            collectJob.cancel()
        }

    @Test
    fun `onReconcileAll with conflicts only exposes hard blocker`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val viewModel = createViewModel()
            val collectJob = collectState(viewModel)

            db.tagsDao().insertIfPossible(Tag(id = "tag-1", order = 1, value = "Health"))
            db.journalEntryDao().insert(
                JournalEntry(
                    id = "entry-1",
                    entryTime = LocalDateTime(2025, 1, 6, 9, 0),
                    text = "Walked",
                    tag = "Health",
                ),
            )
            db.entryConflictDao().insert(
                EntryConflict(
                    id = "conflict-1",
                    entryId = "entry-1",
                    entryTime = LocalDateTime(2025, 1, 6, 9, 0),
                    text = "Ran",
                    tag = "Health",
                    senderName = "Watch",
                ),
            )

            viewModel.state.first { state ->
                state.dateWithCountList.any { it.conflictCount == 1 }
            }

            viewModel.onReconcileAll(upload = false)

            val blockerState = viewModel.state.first { it.reconcileAllDialogState != null }
            assertEquals(
                ReconcileAllDialogState.HardBlocker(untaggedCount = 0, conflictCount = 1),
                blockerState.reconcileAllDialogState,
            )
            assertFalse(db.journalEntryDao().get("entry-1")!!.reconciled)
            collectJob.cancel()
        }

    @Test
    fun `onReconcileAll reconciles and uploads immediately when all days pass with upload true`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val viewModel = createViewModel()
            val collectJob = collectState(viewModel)

            db.tagsDao().insertIfPossible(Tag(id = "tag-1", order = 1, value = "Health"))
            db.journalEntryDao().insert(
                listOf(
                    JournalEntry(
                        id = "mon-1",
                        entryTime = LocalDateTime(2025, 1, 6, 8, 0),
                        text = "Woke up at 7am",
                        tag = "Health",
                    ),
                    JournalEntry(
                        id = "tue-1",
                        entryTime = LocalDateTime(2025, 1, 7, 8, 0),
                        text = "Woke up at 8am",
                        tag = "Health",
                    ),
                ),
            )
            reconcileCheckDao.upsert(
                ReconcileCheck(
                    id = "check-daily",
                    tag = "Health",
                    daysOfWeek = DayOfWeek.entries.toSet(),
                    matchType = ReconcileCheckMatchType.STARTS_WITH,
                    query = "Woke up at",
                    enabled = true,
                ),
            )
            reconcileCheckDao.upsert(
                ReconcileCheck(
                    id = "check-disabled",
                    tag = "Feeling",
                    matchType = ReconcileCheckMatchType.ANY_ENTRY,
                    enabled = false,
                ),
            )
            reconcileCheckDao.upsert(
                ReconcileCheck(
                    id = "check-wed",
                    tag = "Activity",
                    daysOfWeek = setOf(DayOfWeek.WEDNESDAY),
                    matchType = ReconcileCheckMatchType.ANY_ENTRY,
                    enabled = true,
                ),
            )

            viewModel.onDateSelected(mondayDate)
            viewModel.state.first { it.dateWithCountList.size == 2 && it.selectedDate == mondayDate }

            viewModel.onReconcileAll(upload = true)

            viewModel.state.first {
                it.dateWithCountList.isEmpty() &&
                    it.selectedDate == null &&
                    it.notUploadedCount == 0
            }
            assertNull(viewModel.state.value.reconcileAllDialogState)
            assertNull(viewModel.state.value.selectedDate)
            assertTrue(db.journalEntryDao().get("mon-1")!!.reconciled)
            assertTrue(db.journalEntryDao().get("tue-1")!!.reconciled)
            assertEquals(1, dataSendHelper.sendEntriesCallCount)
            assertEquals(setOf("mon-1", "tue-1"), dataSendHelper.entriesSent.map { it.id }.toSet())
            collectJob.cancel()
        }

    @Test
    fun `onReconcileAll reconciles without uploading when all days pass with upload false`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val viewModel = createViewModel()
            val collectJob = collectState(viewModel)

            db.tagsDao().insertIfPossible(Tag(id = "tag-1", order = 1, value = "Health"))
            db.journalEntryDao().insert(
                JournalEntry(
                    id = "mon-1",
                    entryTime = LocalDateTime(2025, 1, 6, 8, 0),
                    text = "Woke up at 7am",
                    tag = "Health",
                ),
            )
            reconcileCheckDao.upsert(
                ReconcileCheck(
                    id = "check-daily",
                    tag = "Health",
                    matchType = ReconcileCheckMatchType.ANY_ENTRY,
                    enabled = true,
                ),
            )

            viewModel.state.first { it.dateWithCountList.size == 1 }

            viewModel.onReconcileAll(upload = false)

            viewModel.state.first { it.dateWithCountList.isEmpty() }
            assertNull(viewModel.state.value.reconcileAllDialogState)
            assertTrue(db.journalEntryDao().get("mon-1")!!.reconciled)
            assertEquals(0, dataSendHelper.sendEntriesCallCount)
            collectJob.cancel()
        }

    @Test
    fun `onReconcileAll exposes soft warning on failing days and dismiss cancels all-or-nothing`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val viewModel = createViewModel()
            val collectJob = collectState(viewModel)

            db.tagsDao().insertIfPossible(Tag(id = "tag-1", order = 1, value = "Health"))
            db.tagsDao().insertIfPossible(Tag(id = "tag-2", order = 2, value = "Activity"))
            db.journalEntryDao().insert(
                listOf(
                    // Monday passes both checks
                    JournalEntry(
                        id = "mon-health",
                        entryTime = LocalDateTime(2025, 1, 6, 8, 0),
                        text = "Woke up at 7",
                        tag = "Health",
                    ),
                    // Tuesday fails Health daily check and Tuesday-only Activity check
                    JournalEntry(
                        id = "tue-activity",
                        entryTime = LocalDateTime(2025, 1, 7, 17, 0),
                        text = "Ran in the park",
                        tag = "Activity",
                    ),
                ),
            )
            val dailyHealthCheck =
                ReconcileCheck(
                    id = "check-health",
                    tag = "Health",
                    daysOfWeek = DayOfWeek.entries.toSet(),
                    matchType = ReconcileCheckMatchType.ANY_ENTRY,
                    enabled = true,
                )
            val tuesdayCommuteCheck =
                ReconcileCheck(
                    id = "check-commute",
                    tag = "Activity",
                    daysOfWeek = setOf(DayOfWeek.TUESDAY),
                    matchType = ReconcileCheckMatchType.CONTAINS_ALL_WORDS,
                    query = "to, work",
                    enabled = true,
                )
            reconcileCheckDao.upsert(dailyHealthCheck)
            reconcileCheckDao.upsert(tuesdayCommuteCheck)

            viewModel.state.first { it.dateWithCountList.size == 2 }

            viewModel.onReconcileAll(upload = true)

            val warningState = viewModel.state.first { it.reconcileAllDialogState != null }
            assertEquals(
                ReconcileAllDialogState.FailedChecks(
                    failedDays =
                        listOf(
                            FailedDayChecks(
                                date = tuesdayDate,
                                failedCheckSummaries =
                                    listOf(
                                        tuesdayCommuteCheck.summary(),
                                        dailyHealthCheck.summary(),
                                    ),
                            ),
                        ),
                    upload = true,
                ),
                warningState.reconcileAllDialogState,
            )
            // Passing Monday entry must remain unreconciled too (all-or-nothing)
            assertFalse(db.journalEntryDao().get("mon-health")!!.reconciled)
            assertFalse(db.journalEntryDao().get("tue-activity")!!.reconciled)
            assertEquals(0, dataSendHelper.sendEntriesCallCount)

            viewModel.dismissReconcileAllDialog()
            val dismissedState = viewModel.state.first { it.reconcileAllDialogState == null }
            assertNull(dismissedState.reconcileAllDialogState)
            assertFalse(db.journalEntryDao().get("mon-health")!!.reconciled)
            assertFalse(db.journalEntryDao().get("tue-activity")!!.reconciled)
            assertEquals(0, dataSendHelper.sendEntriesCallCount)
            collectJob.cancel()
        }

    @Test
    fun `confirmReconcileAllAnyway reconciles and uploads when upload was true`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val viewModel = createViewModel()
            val collectJob = collectState(viewModel)

            db.tagsDao().insertIfPossible(Tag(id = "tag-1", order = 1, value = "Health"))
            db.journalEntryDao().insert(
                listOf(
                    JournalEntry(
                        id = "mon-1",
                        entryTime = LocalDateTime(2025, 1, 6, 8, 0),
                        text = "Water",
                        tag = "Health",
                    ),
                    JournalEntry(
                        id = "tue-1",
                        entryTime = LocalDateTime(2025, 1, 7, 8, 0),
                        text = "Stretch",
                        tag = "Health",
                    ),
                ),
            )
            reconcileCheckDao.upsert(
                ReconcileCheck(
                    id = "check-feeling",
                    tag = "Feeling",
                    daysOfWeek = DayOfWeek.entries.toSet(),
                    matchType = ReconcileCheckMatchType.ANY_ENTRY,
                    enabled = true,
                ),
            )

            viewModel.onDateSelected(tuesdayDate)
            viewModel.state.first { it.dateWithCountList.size == 2 && it.selectedDate == tuesdayDate }

            viewModel.onReconcileAll(upload = true)
            viewModel.state.first { state ->
                val dialog = state.reconcileAllDialogState as? ReconcileAllDialogState.FailedChecks
                dialog != null && dialog.failedDays.map { it.date } == listOf(mondayDate, tuesdayDate) && dialog.upload
            }

            viewModel.confirmReconcileAllAnyway()

            viewModel.state.first {
                it.dateWithCountList.isEmpty() &&
                    it.selectedDate == null &&
                    it.notUploadedCount == 0
            }
            assertNull(viewModel.state.value.reconcileAllDialogState)
            assertNull(viewModel.state.value.selectedDate)
            assertTrue(db.journalEntryDao().get("mon-1")!!.reconciled)
            assertTrue(db.journalEntryDao().get("tue-1")!!.reconciled)
            assertEquals(1, dataSendHelper.sendEntriesCallCount)
            assertEquals(setOf("mon-1", "tue-1"), dataSendHelper.entriesSent.map { it.id }.toSet())
            collectJob.cancel()
        }

    @Test
    fun `confirmReconcileAllAnyway reconciles without uploading when upload was false`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val viewModel = createViewModel()
            val collectJob = collectState(viewModel)

            db.tagsDao().insertIfPossible(Tag(id = "tag-1", order = 1, value = "Health"))
            db.journalEntryDao().insert(
                JournalEntry(
                    id = "mon-1",
                    entryTime = LocalDateTime(2025, 1, 6, 8, 0),
                    text = "Water",
                    tag = "Health",
                ),
            )
            reconcileCheckDao.upsert(
                ReconcileCheck(
                    id = "check-feeling",
                    tag = "Feeling",
                    matchType = ReconcileCheckMatchType.ANY_ENTRY,
                    enabled = true,
                ),
            )

            viewModel.state.first { it.dateWithCountList.size == 1 }

            viewModel.onReconcileAll(upload = false)
            val warningState = viewModel.state.first { it.reconcileAllDialogState != null }
            assertEquals(
                false,
                (warningState.reconcileAllDialogState as ReconcileAllDialogState.FailedChecks).upload,
            )

            viewModel.confirmReconcileAllAnyway()

            viewModel.state.first { it.dateWithCountList.isEmpty() && it.selectedDate == null }
            testScheduler.advanceUntilIdle()
            assertNull(viewModel.state.value.reconcileAllDialogState)
            assertTrue(db.journalEntryDao().get("mon-1")!!.reconciled)
            assertEquals(0, dataSendHelper.sendEntriesCallCount)
            collectJob.cancel()
        }

    private fun TestScope.collectState(viewModel: JournalEntryDaysViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }

    private fun TestScope.createViewModel(): JournalEntryDaysViewModel {
        val webSocketHelper =
            WebSocketHelper(
                httpClient = httpClient,
                json = testJson,
                encryptionHelper = encryptionHelper,
            )
        val dataReceiveHelper =
            object : DataReceiveHelper {
                override val payloadFlow = MutableSharedFlow<Payload>()

                override fun setEntityHandler(handler: suspend (Entity) -> Unit) = Unit
            }
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val verifyEntriesHelper =
            VerifyEntriesHelper(
                coroutineScope = backgroundScope,
                ioDispatcher = dispatcher,
                dataSendHelper = dataSendHelper,
                dataReceiveHelper = dataReceiveHelper,
                repository = repository,
                clock = clock,
            )
        val lifecycleCoordinator =
            AppLifecycleCoordinator(
                coroutineScope = backgroundScope,
                ioDispatcher = dispatcher,
                webSocketHelper = webSocketHelper,
                verifyEntriesHelper = verifyEntriesHelper,
                dataReceiveHelper = dataReceiveHelper,
                dataSendHelper = dataSendHelper,
                repository = repository,
                tagsDao = db.tagsDao(),
                templatesDao = db.templateDao(),
                prefManager = prefManager,
            )
        return JournalEntryDaysViewModel(
            repository = repository,
            reconcileCheckDao = reconcileCheckDao,
            tagsDao = db.tagsDao(),
            verifyEntriesHelper = verifyEntriesHelper,
            webSocketHelper = webSocketHelper,
            lifecycleCoordinator = lifecycleCoordinator,
            clock = clock,
            timeZone = TimeZone.UTC,
        )
    }
}
