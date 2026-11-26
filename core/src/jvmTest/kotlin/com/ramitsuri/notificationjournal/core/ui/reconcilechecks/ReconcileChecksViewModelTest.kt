package com.ramitsuri.notificationjournal.core.ui.reconcilechecks

import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.ReconcileCheckDao
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.data.getTestDb
import com.ramitsuri.notificationjournal.core.model.ReconcileCheck
import com.ramitsuri.notificationjournal.core.model.ReconcileCheckMatchType
import com.ramitsuri.notificationjournal.core.model.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReconcileChecksViewModelTest {
    private lateinit var db: AppDatabase
    private lateinit var reconcileCheckDao: ReconcileCheckDao
    private lateinit var tagsDao: TagsDao
    private lateinit var viewModel: ReconcileChecksViewModel

    @Before
    fun setUp() {
        db = getTestDb()
        reconcileCheckDao = db.reconcileCheckDao()
        tagsDao = db.tagsDao()
        viewModel =
            ReconcileChecksViewModel(
                dao = reconcileCheckDao,
                tagsDao = tagsDao,
            )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `availableTags excludes NO_TAG and missing tag flag reflects TagsDao`() =
        runTest {
            tagsDao.insertIfPossible(Tag(id = "tag-no", order = 0, value = Tag.NO_TAG.value))
            tagsDao.insertIfPossible(Tag(id = "tag-1", order = 1, value = "Health"))

            reconcileCheckDao.upsert(
                ReconcileCheck(
                    id = "check-1",
                    tag = "Health",
                    matchType = ReconcileCheckMatchType.ANY_ENTRY,
                ),
            )
            reconcileCheckDao.upsert(
                ReconcileCheck(
                    id = "check-2",
                    tag = "DeletedTag",
                    matchType = ReconcileCheckMatchType.STARTS_WITH,
                    query = "Commute",
                ),
            )

            val state =
                viewModel.state.first {
                    it.checks.size == 2 && it.availableTags.size == 1
                }
            assertEquals(listOf("Health"), state.availableTags.map { it.value })

            val byId = state.checks.associateBy { it.check.id }
            val existingTagItem = byId.getValue("check-1")
            assertFalse(existingTagItem.isMissingTag)
            assertEquals("Every day • Health • at least 1 entry", existingTagItem.summary)

            val missingTagItem = byId.getValue("check-2")
            assertTrue(missingTagItem.isMissingTag)
            assertEquals("Every day • DeletedTag • starts with \"Commute\"", missingTagItem.summary)
        }

    @Test
    fun `addClicked initializes defaults and requires valid tag, non-empty days, and valid query`() =
        runTest {
            tagsDao.insertIfPossible(Tag(id = "tag-1", order = 1, value = "Health"))
            viewModel.state.first { it.availableTags.size == 1 }

            viewModel.addClicked()
            val initialState = viewModel.state.value
            val initialEditor = initialState.editorState
            assertNotNull(initialEditor)
            assertEquals(DayOfWeek.entries.toSet(), initialEditor?.daysOfWeek)
            assertEquals(ReconcileCheckMatchType.ANY_ENTRY, initialEditor?.matchType)
            assertFalse(initialState.canSave)

            // Rejects Tag.NO_TAG
            viewModel.tagSelected(Tag.NO_TAG.value)
            val editorAfterNoTag = viewModel.state.value.editorState
            assertNull(editorAfterNoTag?.selectedTag)
            assertFalse(viewModel.state.value.canSave)

            // Selecting valid tag enables save for ANY_ENTRY
            viewModel.tagSelected("Health")
            assertTrue(viewModel.state.value.canSave)

            // Removing all days disables save
            DayOfWeek.entries.forEach { day -> viewModel.dayToggled(day) }
            val editorAfterClear = viewModel.state.value.editorState
            assertEquals(emptySet<DayOfWeek>(), editorAfterClear?.daysOfWeek)
            assertFalse(viewModel.state.value.canSave)

            // Re-adding a day restores validity
            viewModel.dayToggled(DayOfWeek.TUESDAY)
            assertTrue(viewModel.state.value.canSave)

            // STARTS_WITH requires non-blank query
            viewModel.matchTypeSelected(ReconcileCheckMatchType.STARTS_WITH)
            assertFalse(viewModel.state.value.canSave)
            viewModel.queryUpdated("   ")
            assertFalse(viewModel.state.value.canSave)
            viewModel.queryUpdated("Woke up at")
            assertTrue(viewModel.state.value.canSave)

            // CONTAINS_ALL_WORDS requires at least one word token
            viewModel.matchTypeSelected(ReconcileCheckMatchType.CONTAINS_ALL_WORDS)
            viewModel.queryUpdated(", , ")
            assertFalse(viewModel.state.value.canSave)
            viewModel.queryUpdated("to, work")
            assertTrue(viewModel.state.value.canSave)
        }

    @Test
    fun `save creates new check and clears query for ANY_ENTRY`() =
        runTest {
            tagsDao.insertIfPossible(Tag(id = "tag-1", order = 1, value = "Feeling"))
            viewModel.state.first { it.availableTags.size == 1 }

            viewModel.addClicked()
            viewModel.tagSelected("Feeling")
            viewModel.matchTypeSelected(ReconcileCheckMatchType.STARTS_WITH)
            viewModel.queryUpdated("  leftover text ")
            viewModel.matchTypeSelected(ReconcileCheckMatchType.ANY_ENTRY)
            viewModel.save()

            val state = viewModel.state.first { it.checks.size == 1 && it.editorState == null }
            val saved = state.checks.single().check
            assertEquals("Feeling", saved.tag)
            assertEquals(DayOfWeek.entries.toSet(), saved.daysOfWeek)
            assertEquals(ReconcileCheckMatchType.ANY_ENTRY, saved.matchType)
            assertEquals("", saved.query)
            assertTrue(saved.enabled)
        }

    @Test
    fun `editClicked updates existing check preserving id and enabled state`() =
        runTest {
            tagsDao.insertIfPossible(Tag(id = "tag-1", order = 1, value = "Activity"))
            tagsDao.insertIfPossible(Tag(id = "tag-2", order = 2, value = "Health"))
            val existing =
                ReconcileCheck(
                    id = "existing-id",
                    tag = "Activity",
                    daysOfWeek = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                    matchType = ReconcileCheckMatchType.CONTAINS_ALL_WORDS,
                    query = "to, work",
                    enabled = false,
                )
            reconcileCheckDao.upsert(existing)
            viewModel.state.first { it.checks.size == 1 && it.availableTags.size == 2 }

            viewModel.editClicked(existing)
            viewModel.tagSelected("Health")
            viewModel.dayToggled(DayOfWeek.WEDNESDAY)
            viewModel.matchTypeSelected(ReconcileCheckMatchType.STARTS_WITH)
            viewModel.queryUpdated("  Ran 5k  ")
            viewModel.save()

            val state =
                viewModel.state.first { current ->
                    val singleItem = current.checks.singleOrNull()
                    current.editorState == null && singleItem?.check?.tag == "Health"
                }
            val updated = state.checks.single().check
            assertEquals("existing-id", updated.id)
            assertEquals("Health", updated.tag)
            assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY), updated.daysOfWeek)
            assertEquals(ReconcileCheckMatchType.STARTS_WITH, updated.matchType)
            assertEquals("Ran 5k", updated.query)
            assertFalse(updated.enabled)
        }

    @Test
    fun `cancel, toggleEnabled, and delete update state and dao`() =
        runTest {
            tagsDao.insertIfPossible(Tag(id = "tag-1", order = 1, value = "Health"))
            val check =
                ReconcileCheck(
                    id = "check-1",
                    tag = "Health",
                    matchType = ReconcileCheckMatchType.ANY_ENTRY,
                    enabled = true,
                )
            reconcileCheckDao.upsert(check)
            viewModel.state.first { it.checks.size == 1 }

            viewModel.editClicked(check)
            assertNotNull(viewModel.state.value.editorState)
            viewModel.queryUpdated("ignored")
            viewModel.onAddOrEditCanceled()
            assertNull(viewModel.state.value.editorState)

            viewModel.toggleEnabled(check, enabled = false)
            val disabledState =
                viewModel.state.first { current ->
                    val singleItem = current.checks.singleOrNull()
                    singleItem?.check?.enabled == false
                }
            val disabledCheck = disabledState.checks.single().check
            assertFalse(disabledCheck.enabled)

            viewModel.delete(disabledCheck)
            val deletedState = viewModel.state.first { it.checks.isEmpty() }
            assertTrue(deletedState.checks.isEmpty())
        }
}
