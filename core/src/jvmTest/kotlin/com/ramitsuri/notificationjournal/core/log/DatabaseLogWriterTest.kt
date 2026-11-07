package com.ramitsuri.notificationjournal.core.log

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.ramitsuri.notificationjournal.core.data.getTestDb
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseLogWriterTest {
    @Test
    fun testLogReceivesMessageTagAndSeverity() =
        runTest {
            val testDispatcher = UnconfinedTestDispatcher(testScheduler)
            val testScope = TestScope(testDispatcher)
            val db = getTestDb()
            val writer =
                DatabaseLogWriter(
                    logDao = db.logDao(),
                    coroutineScope = testScope,
                )
            Logger.setLogWriters(writer)

            val exception = RuntimeException("Test error message")
            Logger.withTag("DatabaseTag").e(exception) { "Something failed" }

            val logs = writer.logs.first { it.isNotEmpty() }
            assertEquals(1, logs.size)
            assertEquals("DatabaseTag", logs[0].tag)
            assertEquals("Something failed", logs[0].message)
            assertEquals(Severity.Error, logs[0].severity)
            assertEquals("Test error message", logs[0].errorMessage)
            assertNotNull(logs[0].stackTrace)

            val tags = writer.getTags().first()
            assertTrue(tags.contains("DatabaseTag"))

            writer.clear()
            val clearedLogs = writer.logs.first { it.isEmpty() }
            assertTrue(clearedLogs.isEmpty())
        }

    @Test
    fun testPruningPrioritizesClearingDebugFirstThenInfo() =
        runTest {
            val testDispatcher = UnconfinedTestDispatcher(testScheduler)
            val testScope = TestScope(testDispatcher)
            val db = getTestDb()
            val logDao = db.logDao()
            val writer =
                DatabaseLogWriter(
                    logDao = logDao,
                    coroutineScope = testScope,
                )
            Logger.setLogWriters(writer)

            // Insert: 1 Error, 2 Info, 2 Debug
            Logger.withTag("Tag").e { "Error 1" }
            Logger.withTag("Tag").i { "Info 1" }
            Logger.withTag("Tag").i { "Info 2" }
            Logger.withTag("Tag").d { "Debug 1" }
            Logger.withTag("Tag").d { "Debug 2" }

            val allLogs = writer.logs.first { it.size == 5 }
            assertEquals(5, allLogs.size)

            // Prune to 3: Should keep 1 Error and 2 Info, dropping the 2 Debug logs
            logDao.pruneOldLogs(maxLogs = 3)
            val logsAfterFirstPrune = writer.logs.first { it.size == 3 }
            assertEquals(3, logsAfterFirstPrune.size)
            assertTrue(logsAfterFirstPrune.any { it.severity == Severity.Error && it.message == "Error 1" })
            assertTrue(logsAfterFirstPrune.any { it.severity == Severity.Info && it.message == "Info 1" })
            assertTrue(logsAfterFirstPrune.any { it.severity == Severity.Info && it.message == "Info 2" })
            assertTrue(logsAfterFirstPrune.none { it.severity == Severity.Debug })

            // Prune to 1: Should keep the Error log and drop the 2 Info logs
            logDao.pruneOldLogs(maxLogs = 1)
            val logsAfterSecondPrune = writer.logs.first { it.size == 1 }
            assertEquals(1, logsAfterSecondPrune.size)
            assertEquals(Severity.Error, logsAfterSecondPrune[0].severity)
            assertEquals("Error 1", logsAfterSecondPrune[0].message)
        }
}
