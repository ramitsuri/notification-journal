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
            assertEquals("Went for a 5k run this morning", result.intent.entryText)
            org.junit.Assert.assertNull(result.intent.tag)
            assertEquals("Rule-based fallback", result.engine)
        }

    @Test
    fun testAddEntryWithPrefix() =
        runTest {
            val result = extractor.extractIntent("log Went for a 5k run this morning")
            assertNotNull(result)
            assertEquals("Went for a 5k run this morning", result.intent.entryText)
        }

    @Test
    fun testAddEntryWithCommandPhrase() =
        runTest {
            val result = extractor.extractIntent("add a new entry that I slept well")
            assertNotNull(result)
            assertEquals("I slept well", result.intent.entryText)
        }

    @Test
    fun testAddEntryWithWriteDownThat() =
        runTest {
            val result = extractor.extractIntent("write down that we had tacos for dinner")
            assertNotNull(result)
            assertEquals("we had tacos for dinner", result.intent.entryText)
        }

    @Test
    fun testAddEntryYesterday() =
        runTest {
            val result = extractor.extractIntent("Ran 5 miles yesterday")
            assertNotNull(result)
            assertEquals("Ran 5 miles yesterday", result.intent.entryText)
            assertEquals(LocalDate.now().minusDays(1).toString(), result.intent.targetDate)
        }

    @Test
    fun testAddEntryDinnerAtTime() =
        runTest {
            val result = extractor.extractIntent("dinner at time")
            assertNotNull(result)
            assertEquals("dinner at {{time}}", result.intent.entryText)
        }

    @Test
    fun testAddEntryWithTargetTime() =
        runTest {
            val result = extractor.extractIntent("Had dinner at 8pm")
            assertNotNull(result)
            assertEquals("20:00", result.intent.targetTime)
        }

    @Test
    fun testAddEntryWithTargetTimeMinutes() =
        runTest {
            val result = extractor.extractIntent("woke up at 7:30 am")
            assertNotNull(result)
            assertEquals("07:30", result.intent.targetTime)
        }

    @Test
    fun testIsAvailable() =
        runTest {
            assertTrue(extractor.isAvailable())
        }
}
