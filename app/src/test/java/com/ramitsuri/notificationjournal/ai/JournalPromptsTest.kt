package com.ramitsuri.notificationjournal.ai

import com.ramitsuri.notificationjournal.ai.prompt.JournalPrompts
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalPromptsTest {
    @Test
    fun testPromptIsSpecificToAddEntries() {
        val prompt = JournalPrompts.getAddEntryStructuredOutputPrompt("test input", "2026-11-06")
        assertTrue(prompt.contains("specialized in adding journal entries"))
        assertFalse(prompt.contains("QUERY_ENTRIES"))
        assertFalse(prompt.contains("searchKeyword"))
        assertFalse(prompt.contains("action"))
    }

    @Test
    fun testPromptIncludesTargetTime() {
        val prompt = JournalPrompts.getAddEntryStructuredOutputPrompt("dinner at 8pm", "2026-11-06")
        assertTrue(prompt.contains("targetTime"))
        assertTrue(prompt.contains("24-hour format"))
    }

    @Test
    fun testPromptIncludesDinnerAtTimeRule() {
        val prompt = JournalPrompts.getAddEntryStructuredOutputPrompt("dinner at time", "2026-11-06")
        assertTrue(prompt.contains("dinner at time"))
        assertTrue(prompt.contains("{{time}}"))
    }

    @Test
    fun testJsonPromptIncludesTargetTime() {
        val jsonPrompt = JournalPrompts.getAddEntryJsonPrompt("dinner at time", "2026-11-06")
        assertTrue(jsonPrompt.contains("\"targetTime\": null"))
        assertFalse(jsonPrompt.contains("\"searchKeyword\""))
        assertFalse(jsonPrompt.contains("\"action\""))
    }

    @Test
    fun testPromptIncludesCustomRules() {
        val prompt =
            JournalPrompts.getAddEntryStructuredOutputPrompt(
                userInput = "went to gym",
                today = "2026-11-06",
                customRules = "When I say gym, tag as #fitness\nReplace coffee with tea",
            )
        assertTrue(prompt.contains("Additional User Custom Rules:"))
        assertTrue(prompt.contains("- When I say gym, tag as #fitness"))
        assertTrue(prompt.contains("- Replace coffee with tea"))
    }

    @Test
    fun testPromptWithoutCustomRulesHasNoCustomRulesSection() {
        val prompt = JournalPrompts.getAddEntryStructuredOutputPrompt("just a note", "2026-11-06", customRules = "")
        assertFalse(prompt.contains("Additional User Custom Rules:"))
    }
}
