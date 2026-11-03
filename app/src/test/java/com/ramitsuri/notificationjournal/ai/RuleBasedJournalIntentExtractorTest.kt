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
            assertEquals("ADD_ENTRY", result.intent.action)
            assertEquals("Went for a 5k run this morning", result.intent.entryText)
            assertEquals("Personal", result.intent.tag)
            assertEquals("Rule-based fallback", result.engine)
        }

    @Test
    fun testAddEntryWithPrefix() =
        runTest {
            val result = extractor.extractIntent("log Went for a 5k run this morning")
            assertNotNull(result)
            assertEquals("ADD_ENTRY", result.intent.action)
            assertEquals("Went for a 5k run this morning", result.intent.entryText)
        }

    @Test
    fun testAddEntryWithCommandPhrase() =
        runTest {
            val result = extractor.extractIntent("add a new entry that I slept well")
            assertNotNull(result)
            assertEquals("ADD_ENTRY", result.intent.action)
            assertEquals("I slept well", result.intent.entryText)
        }

    @Test
    fun testAddEntryWithWriteDownThat() =
        runTest {
            val result = extractor.extractIntent("write down that we had tacos for dinner")
            assertNotNull(result)
            assertEquals("ADD_ENTRY", result.intent.action)
            assertEquals("we had tacos for dinner", result.intent.entryText)
        }

    @Test
    fun testQueryEntriesKeyword() =
        runTest {
            val result = extractor.extractIntent("Find run")
            assertNotNull(result)
            assertEquals("QUERY_ENTRIES", result.intent.action)
            assertEquals("run", result.intent.searchKeyword)
        }

    @Test
    fun testQueryEntriesYesterday() =
        runTest {
            val result = extractor.extractIntent("What did I write yesterday?")
            assertNotNull(result)
            assertEquals("QUERY_ENTRIES", result.intent.action)
            assertEquals(LocalDate.now().minusDays(1).toString(), result.intent.targetDate)
        }

    @Test
    fun testIsAvailable() =
        runTest {
            assertTrue(extractor.isAvailable())
        }
}
