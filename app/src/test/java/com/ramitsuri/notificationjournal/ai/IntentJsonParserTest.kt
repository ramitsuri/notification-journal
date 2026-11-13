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
            """{"entryText":"went for a run","tag":null,"targetDate":"2026-11-13","targetTime":"08:00","conversationalReply":"Saved."}"""
        val result = IntentJsonParser.parseJson(raw, "original input")
        assertNotNull(result)
        assertEquals("went for a run", result?.entryText)
        assertNull(result?.tag)
        assertEquals("2026-11-13", result?.targetDate)
        assertEquals("08:00", result?.targetTime)
        assertEquals("Saved.", result?.conversationalReply)
    }

    @Test
    fun testParseMarkdownFencedJson() {
        val raw =
            """
            Here is your output:
            ```json
            {
              "entryText": "Dinner with friends",
              "tag": null,
              "targetDate": null,
              "targetTime": "19:30",
              "conversationalReply": "Noted."
            }
            ```
            Hope this helps!
            """.trimIndent()
        val result = IntentJsonParser.parseJson(raw, "original input")
        assertNotNull(result)
        assertEquals("Dinner with friends", result?.entryText)
        assertEquals("19:30", result?.targetTime)
    }

    @Test
    fun testParseEmptyEntryTextFallsBackToOriginalInput() {
        val raw = """{"entryText":"","tag":null,"targetDate":null,"targetTime":null,"conversationalReply":null}"""
        val result = IntentJsonParser.parseJson(raw, "Spoken voice note")
        assertNotNull(result)
        assertEquals("Spoken voice note", result?.entryText)
    }

    @Test
    fun testParseInvalidJsonReturnsNull() {
        val raw = "I don't know how to format this as json."
        val result = IntentJsonParser.parseJson(raw, "original input")
        assertNull(result)
    }
}
