package com.ramitsuri.notificationjournal.core.data

import com.ramitsuri.notificationjournal.core.model.ReconcileCheck
import com.ramitsuri.notificationjournal.core.model.ReconcileCheckMatchType
import com.ramitsuri.notificationjournal.core.model.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconcileCheckDaoTest {
    @Test
    fun `upsert insert update delete and getEnabled work correctly`() =
        runTest {
            val db = getTestDb()
            val dao = db.reconcileCheckDao()

            val check1 =
                ReconcileCheck(
                    id = "check-1",
                    tag = "Feeling",
                    daysOfWeek = DayOfWeek.entries.toSet(),
                    matchType = ReconcileCheckMatchType.ANY_ENTRY,
                    query = "",
                    enabled = true,
                )
            val check2 =
                ReconcileCheck(
                    id = "check-2",
                    tag = "Activity",
                    daysOfWeek = setOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
                    matchType = ReconcileCheckMatchType.CONTAINS_ALL_WORDS,
                    query = "to, work",
                    enabled = false,
                )

            dao.upsert(check1)
            dao.insert(check2)

            val allChecks = dao.getAllFlow().first()
            assertEquals(listOf(check2, check1), allChecks)
            assertEquals(listOf(check1), dao.getEnabled())

            val updatedCheck2 =
                check2.copy(
                    matchType = ReconcileCheckMatchType.STARTS_WITH,
                    query = "Drove to",
                    enabled = true,
                )
            dao.update(updatedCheck2)

            assertEquals(listOf(updatedCheck2, check1), dao.getAllFlow().first())
            assertEquals(listOf(updatedCheck2, check1), dao.getEnabled())

            val upsertedCheck1 = check1.copy(enabled = false)
            dao.upsert(upsertedCheck1)
            assertEquals(listOf(updatedCheck2), dao.getEnabled())

            dao.delete(upsertedCheck1)
            assertEquals(listOf(updatedCheck2), dao.getAllFlow().first())

            dao.delete(updatedCheck2)
            assertTrue(dao.getAllFlow().first().isEmpty())
            assertTrue(dao.getEnabled().isEmpty())
        }

    @Test
    fun `tags clearAndInsert does not affect reconcile checks`() =
        runTest {
            val db = getTestDb()
            val tagsDao = db.tagsDao()
            val checkDao = db.reconcileCheckDao()

            val tag = Tag(id = "tag-1", order = 1, value = "Health")
            tagsDao.insertIfPossible(tag)

            val check =
                ReconcileCheck(
                    id = "check-1",
                    tag = "Health",
                    daysOfWeek = DayOfWeek.entries.toSet(),
                    matchType = ReconcileCheckMatchType.STARTS_WITH,
                    query = "Woke up",
                    enabled = true,
                )
            checkDao.upsert(check)

            // Syncing tags clears and re-inserts Tags table; ReconcileCheck has no FK so it survives.
            tagsDao.clearAndInsert(listOf(Tag(id = "tag-2", order = 1, value = "Work")))

            assertEquals(listOf(check), checkDao.getAllFlow().first())
        }
}
