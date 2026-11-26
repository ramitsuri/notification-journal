package com.ramitsuri.notificationjournal.core.ui.journalentry

import com.ramitsuri.notificationjournal.core.data.ReconcileCheckDao
import com.ramitsuri.notificationjournal.core.di.PlatformConfig
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JournalEntryViewModelTest : BaseTest() {
    private lateinit var reconcileCheckDao: ReconcileCheckDao
    private lateinit var httpClient: HttpClient

    // Tuesday, Jan 7, 2025
    private val testDate = LocalDate(2025, 1, 7)

    @Before
    fun setUp() {
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
    fun `onReconcile with untagged entries exposes hard blocker and does not reconcile`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

            val untaggedEntry =
                JournalEntry(
                    id = "entry-untagged",
                    entryTime = LocalDateTime(2025, 1, 7, 9, 0),
                    text = "Quick note",
                    tag = Tag.NO_TAG.value,
                )
            db.journalEntryDao().insert(untaggedEntry)
            viewModel.state.first { state ->
                val dayGroup = state.dayGroup
                dayGroup != null && dayGroup.date == testDate && dayGroup.untaggedCount == 1
            }

            viewModel.onReconcile()

            val blockerState =
                viewModel.state.first { it.reconcileDialogState != null }
            assertEquals(
                ReconcileDialogState.HardBlocker(untaggedCount = 1, conflictCount = 0),
                blockerState.reconcileDialogState,
            )
            assertFalse(db.journalEntryDao().get("entry-untagged")!!.reconciled)

            viewModel.dismissReconcileDialog()
            val dismissedState = viewModel.state.first { it.reconcileDialogState == null }
            assertNull(dismissedState.reconcileDialogState)
            collectJob.cancel()
        }

    @Test
    fun `onReconcile with unresolved conflicts exposes hard blocker and does not reconcile`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

            db.tagsDao().insertIfPossible(Tag(id = "tag-1", order = 1, value = "Health"))
            val entry =
                JournalEntry(
                    id = "entry-1",
                    entryTime = LocalDateTime(2025, 1, 7, 9, 0),
                    text = "Woke up at 7",
                    tag = "Health",
                )
            db.journalEntryDao().insert(entry)
            db.entryConflictDao().insert(
                EntryConflict(
                    id = "conflict-1",
                    entryId = "entry-1",
                    entryTime = LocalDateTime(2025, 1, 7, 9, 0),
                    text = "Woke up at 8",
                    tag = "Health",
                    senderName = "Phone",
                ),
            )

            val loadedState =
                viewModel.state.first {
                    it.dayGroup?.date == testDate && it.dayGroupConflictCount == 1
                }
            assertEquals(1, loadedState.dayGroupConflictCount)

            viewModel.onReconcile()

            val blockerState = viewModel.state.first { it.reconcileDialogState != null }
            assertEquals(
                ReconcileDialogState.HardBlocker(untaggedCount = 0, conflictCount = 1),
                blockerState.reconcileDialogState,
            )
            assertFalse(db.journalEntryDao().get("entry-1")!!.reconciled)
            collectJob.cancel()
        }

    @Test
    fun `onReconcile with both untagged entries and conflicts reports both in hard blocker`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

            db.tagsDao().insertIfPossible(Tag(id = "tag-1", order = 1, value = "Health"))
            db.journalEntryDao().insert(
                JournalEntry(
                    id = "entry-tagged",
                    entryTime = LocalDateTime(2025, 1, 7, 9, 0),
                    text = "Ran",
                    tag = "Health",
                ),
            )
            db.journalEntryDao().insert(
                JournalEntry(
                    id = "entry-untagged",
                    entryTime = LocalDateTime(2025, 1, 7, 10, 0),
                    text = "Untagged note",
                    tag = Tag.NO_TAG.value,
                ),
            )
            db.entryConflictDao().insert(
                EntryConflict(
                    id = "conflict-1",
                    entryId = "entry-tagged",
                    entryTime = LocalDateTime(2025, 1, 7, 9, 0),
                    text = "Walked",
                    tag = "Health",
                    senderName = "Watch",
                ),
            )

            viewModel.state.first { state ->
                val dayGroup = state.dayGroup
                dayGroup != null &&
                    dayGroup.date == testDate &&
                    dayGroup.untaggedCount == 1 &&
                    state.dayGroupConflictCount == 1
            }

            viewModel.onReconcile()

            val blockerState = viewModel.state.first { it.reconcileDialogState != null }
            assertEquals(
                ReconcileDialogState.HardBlocker(untaggedCount = 1, conflictCount = 1),
                blockerState.reconcileDialogState,
            )
            collectJob.cancel()
        }

    @Test
    fun `onReconcile reconciles immediately when all enabled checks pass`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

            db.tagsDao().insertIfPossible(Tag(id = "tag-1", order = 1, value = "Health"))
            db.journalEntryDao().insert(
                JournalEntry(
                    id = "entry-1",
                    entryTime = LocalDateTime(2025, 1, 7, 8, 0),
                    text = "Woke up at 7am",
                    tag = "Health",
                ),
            )
            val passingCheck =
                ReconcileCheck(
                    id = "check-pass",
                    tag = "Health",
                    daysOfWeek = setOf(DayOfWeek.TUESDAY),
                    matchType = ReconcileCheckMatchType.STARTS_WITH,
                    query = "Woke up at",
                    enabled = true,
                )
            val disabledCheck =
                ReconcileCheck(
                    id = "check-disabled",
                    tag = "Feeling",
                    matchType = ReconcileCheckMatchType.ANY_ENTRY,
                    enabled = false,
                )
            val otherDayCheck =
                ReconcileCheck(
                    id = "check-monday",
                    tag = "Activity",
                    daysOfWeek = setOf(DayOfWeek.MONDAY),
                    matchType = ReconcileCheckMatchType.ANY_ENTRY,
                    enabled = true,
                )
            reconcileCheckDao.upsert(passingCheck)
            reconcileCheckDao.upsert(disabledCheck)
            reconcileCheckDao.upsert(otherDayCheck)

            viewModel.state.first { state ->
                val dayGroup = state.dayGroup
                dayGroup != null &&
                    dayGroup.date == testDate &&
                    dayGroup.tagGroups.any { group -> group.entries.isNotEmpty() }
            }

            viewModel.onReconcile()

            db.journalEntryDao().getForDateFlow(testDate.toString()).first { entries ->
                entries.isNotEmpty() && entries.all { it.reconciled }
            }
            assertNull(viewModel.state.value.reconcileDialogState)
            assertTrue(db.journalEntryDao().get("entry-1")!!.reconciled)
            collectJob.cancel()
        }

    @Test
    fun `onReconcile exposes soft warning on failed checks and dismiss cancels without reconciling`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

            db.tagsDao().insertIfPossible(Tag(id = "tag-1", order = 1, value = "Health"))
            db.journalEntryDao().insert(
                JournalEntry(
                    id = "entry-1",
                    entryTime = LocalDateTime(2025, 1, 7, 8, 0),
                    text = "Drank water",
                    tag = "Health",
                ),
            )
            val failingCheck =
                ReconcileCheck(
                    id = "check-fail",
                    tag = "Feeling",
                    daysOfWeek = DayOfWeek.entries.toSet(),
                    matchType = ReconcileCheckMatchType.ANY_ENTRY,
                    enabled = true,
                )
            reconcileCheckDao.upsert(failingCheck)

            viewModel.state.first { state ->
                val dayGroup = state.dayGroup
                dayGroup != null &&
                    dayGroup.date == testDate &&
                    dayGroup.tagGroups.any { group -> group.entries.isNotEmpty() }
            }

            viewModel.onReconcile()

            val warningState = viewModel.state.first { it.reconcileDialogState != null }
            assertEquals(
                ReconcileDialogState.FailedChecks(
                    date = testDate,
                    failedCheckSummaries = listOf(failingCheck.summary()),
                ),
                warningState.reconcileDialogState,
            )
            assertFalse(db.journalEntryDao().get("entry-1")!!.reconciled)

            viewModel.dismissReconcileDialog()
            val dismissedState = viewModel.state.first { it.reconcileDialogState == null }
            assertNull(dismissedState.reconcileDialogState)
            assertFalse(db.journalEntryDao().get("entry-1")!!.reconciled)
            collectJob.cancel()
        }

    @Test
    fun `confirmReconcileAnyway reconciles after soft warning`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

            db.tagsDao().insertIfPossible(Tag(id = "tag-1", order = 1, value = "Health"))
            db.journalEntryDao().insert(
                JournalEntry(
                    id = "entry-1",
                    entryTime = LocalDateTime(2025, 1, 7, 8, 0),
                    text = "Drank water",
                    tag = "Health",
                ),
            )
            val failingCheck =
                ReconcileCheck(
                    id = "check-fail",
                    tag = "Activity",
                    daysOfWeek = setOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
                    matchType = ReconcileCheckMatchType.CONTAINS_ALL_WORDS,
                    query = "to, work",
                    enabled = true,
                )
            reconcileCheckDao.upsert(failingCheck)

            viewModel.state.first { state ->
                val dayGroup = state.dayGroup
                dayGroup != null &&
                    dayGroup.date == testDate &&
                    dayGroup.tagGroups.any { group -> group.entries.isNotEmpty() }
            }

            viewModel.onReconcile()
            val warningState = viewModel.state.first { it.reconcileDialogState != null }
            assertEquals(
                ReconcileDialogState.FailedChecks(
                    date = testDate,
                    failedCheckSummaries = listOf("Tue, Wed • Activity • contains \"to\", \"work\""),
                ),
                warningState.reconcileDialogState,
            )

            viewModel.confirmReconcileAnyway()

            db.journalEntryDao().getForDateFlow(testDate.toString()).first { entries ->
                entries.isNotEmpty() && entries.all { it.reconciled }
            }
            assertNull(viewModel.state.value.reconcileDialogState)
            assertTrue(db.journalEntryDao().get("entry-1")!!.reconciled)
            collectJob.cancel()
        }

    @Test
    fun `onReconcile on empty day group is a no-op even when checks are configured`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val viewModel = createViewModel()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

            reconcileCheckDao.upsert(
                ReconcileCheck(
                    id = "check-1",
                    tag = "Feeling",
                    matchType = ReconcileCheckMatchType.ANY_ENTRY,
                    enabled = true,
                ),
            )
            viewModel.state.first { it.dayGroup?.date == testDate }

            viewModel.onReconcile()
            testScheduler.advanceUntilIdle()

            assertNull(viewModel.state.value.reconcileDialogState)
            collectJob.cancel()
        }

    private fun TestScope.createViewModel(): JournalEntryViewModel {
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
        return JournalEntryViewModel(
            initialDate = testDate,
            repository = repository,
            exportRepository = null,
            tagsDao = db.tagsDao(),
            reconcileCheckDao = reconcileCheckDao,
            prefManager = prefManager,
            webSocketHelper = webSocketHelper,
            voiceNoteDao = db.voiceNoteDao(),
            lifecycleCoordinator = lifecycleCoordinator,
            platformConfig = PlatformConfig(isDesktop = true, allowJournalImport = true),
            notificationHelper = null,
        )
    }
}
