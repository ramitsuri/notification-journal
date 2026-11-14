package com.ramitsuri.notificationjournal.ai

import com.ramitsuri.notificationjournal.ai.extractor.IntentJsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class IntentJsonParserTest {
    @Test
    fun testParseCleanJson() {
        val raw =
            """{"entryText":"went for a run","targetDate":"2026-11-13","targetTime":"08:00"}"""
        val result = IntentJsonParser.parseJson(raw, "original input")
        assertNotNull(result)
        assertEquals("went for a run", result?.entryText)
        assertEquals("2026-11-13", result?.targetDate)
        assertEquals("08:00", result?.targetTime)
    }

    @Test
    fun testParseMarkdownFencedJson() {
        val raw =
            """
            Here is your output:
            ```json
            {
              "entryText": "Dinner with friends",
              "targetDate": null,
              "targetTime": "19:30"
            }
            ```
            Hope this helps!
            """.trimIndent()
        val result = IntentJsonParser.parseJson(raw, "original input")
        assertNotNull(result)
        assertEquals("Dinner with friends", result?.entryText)
        assertEquals("19:30", result?.targetTime)
        assertNull(result?.targetDate)
    }

    @Test
    fun testParseEmptyEntryTextFallsBackToOriginalInput() {
        val raw = """{"entryText":"","targetDate":null,"targetTime":null}"""
        val result = IntentJsonParser.parseJson(raw, "Spoken voice note")
        assertNotNull(result)
        assertEquals("Spoken voice note", result?.entryText)
        assertNull(result?.targetDate)
        assertNull(result?.targetTime)
    }

    @Test
    fun testParseInvalidJsonReturnsNull() {
        val raw = "I don't know how to format this as json."
        val result = IntentJsonParser.parseJson(raw, "original input")
        assertNull(result)
    }
}
