package com.ramitsuri.notificationjournal.ai

import com.ramitsuri.notificationjournal.ai.prompt.JournalPrompts
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class JournalPromptsTest {
    @Test
    fun testPromptIncludesContextAndRules() {
        val today = LocalDate.of(2026, 11, 13)
        val prompt = JournalPrompts.getAddEntryPrompt("test input", today = today)
        assertTrue(prompt.contains("Today: 2026-11-13"))
        assertTrue(prompt.contains("Yesterday: 2026-11-12"))
        assertTrue(prompt.contains("targetDate"))
        assertTrue(prompt.contains("targetTime"))
        assertTrue(prompt.contains("entryText"))
        assertTrue(prompt.contains("Schema:"))
    }

    @Test
    fun testPromptEscapesDoubleQuotes() {
        val prompt = JournalPrompts.getAddEntryPrompt("said \"hello\" to world")
        assertTrue(prompt.contains("said 'hello' to world"))
        assertFalse(prompt.contains("said \"hello\" to world"))
    }

    @Test
    fun testPromptIncludesLiteralTimeRule() {
        val prompt = JournalPrompts.getAddEntryPrompt("dinner at time")
        assertTrue(prompt.contains("Literal time placeholder"))
        assertTrue(prompt.contains("{{time}}"))
    }

    @Test
    fun testPromptIncludesCustomRules() {
        val prompt =
            JournalPrompts.getAddEntryPrompt(
                userInput = "went to gym",
                today = LocalDate.of(2026, 11, 13),
                customRules = "When I say gym, tag as #fitness\nReplace coffee with tea",
            )
        assertTrue(prompt.contains("Additional Rules:"))
        assertTrue(prompt.contains("- When I say gym, tag as #fitness"))
        assertTrue(prompt.contains("- Replace coffee with tea"))
    }

    @Test
    fun testPromptWithoutCustomRulesHasNoCustomRulesSection() {
        val prompt = JournalPrompts.getAddEntryPrompt("just a note", customRules = "")
        assertFalse(prompt.contains("Additional Rules:"))
    }
}
