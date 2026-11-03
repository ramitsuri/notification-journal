package com.ramitsuri.notificationjournal.ai

import com.ramitsuri.notificationjournal.ai.extractor.RuleBasedJournalIntentExtractor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RuleBasedJournalIntentExtractorTest {
    private val extractor = RuleBasedJournalIntentExtractor()

    @Test
    fun testAddEntryExtraction() =
        runTest {
            val result = extractor.extractIntent("Went for a 5k run this morning")
            assertNotNull(result)
            assertEquals("ADD_ENTRY", result.action)
            assertEquals("Went for a 5k run this morning", result.entryText)
            assertEquals("Personal", result.tag)
        }

    @Test
    fun testAddEntryWithPrefix() =
        runTest {
            val result = extractor.extractIntent("log Went for a 5k run this morning")
            assertNotNull(result)
            assertEquals("ADD_ENTRY", result.action)
            assertEquals("Went for a 5k run this morning", result.entryText)
        }

    @Test
    fun testQueryEntriesKeyword() =
        runTest {
            val result = extractor.extractIntent("Find run")
            assertNotNull(result)
            assertEquals("QUERY_ENTRIES", result.action)
            assertEquals("run", result.searchKeyword)
        }

    @Test
    fun testQueryEntriesYesterday() =
        runTest {
            val result = extractor.extractIntent("What did I write yesterday?")
            assertNotNull(result)
            assertEquals("QUERY_ENTRIES", result.action)
            assertEquals(LocalDate.now().minusDays(1).toString(), result.targetDate)
        }

    @Test
    fun testIsAvailable() =
        runTest {
            assertTrue(extractor.isAvailable())
        }
}
