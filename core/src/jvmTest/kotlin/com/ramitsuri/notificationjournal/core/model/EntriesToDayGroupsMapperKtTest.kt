package com.ramitsuri.notificationjournal.core.model

import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class EntriesToDayGroupsMapperKtTest {
    @Test
    fun testWithDifferentTags() {
        val tags = listOf(
            Tag(id = "id", value = "tag1", order = 1),
            Tag(id = "id", value = "tag2", order = 2),
        )
        val entries = listOf(
            entry(tag = tags[0].value),
            entry(tag = tags[1].value),
        )

        val dayGroup = entries.toDayGroups().first()

        val tagGroup1 = dayGroup.tagGroups.first { it.tag == "tag1" }
        assertEquals(1, tagGroup1.entries.size)
        assertEquals(entries.first { it.tag == "tag1" }, tagGroup1.entries.first())

        val tagGroup2 = dayGroup.tagGroups.first { it.tag == "tag2" }
        assertEquals(1, tagGroup2.entries.size)
        assertEquals(entries.first { it.tag == "tag2" }, tagGroup2.entries.first())
    }

    @Test
    fun testWithNoTagVariationTags() {
        val entries = listOf(
            entry(tag = null),
            entry(tag = Tag.NO_TAG.value),
        )

        val dayGroup = entries.toDayGroups().first()

        val tagGroup = dayGroup.tagGroups.first()
        assertEquals(2, tagGroup.entries.size)
    }

    @Test
    fun testWithNoTagAndTag() {
        val entries = listOf(
            entry(tag = "tag1"),
            entry(tag = null),
            entry(tag = Tag.NO_TAG.value),
        )

        val dayGroup = entries.toDayGroups().first()

        val tagGroup1 = dayGroup.tagGroups.first { it.tag == "tag1" }
        assertEquals(1, tagGroup1.entries.size)

        val tagGroupNoTag = dayGroup.tagGroups.first { it.tag != "tag1" }
        assertEquals(2, tagGroupNoTag.entries.size)

        assertEquals(Tag.NO_TAG.value, dayGroup.tagGroups.first().tag)
    }

    @Test
    fun testSortOrderWithNoTagAndTag() {
        val entries = listOf(
            entry(tag = "tag1"),
            entry(tag = null),
            entry(tag = Tag.NO_TAG.value),
        )

        val dayGroup = entries.toDayGroups().first()

        val tagGroup1 = dayGroup.tagGroups[0]
        assertEquals(Tag.NO_TAG.value, tagGroup1.tag)

        val tagGroup2 = dayGroup.tagGroups[1]
        assertEquals("tag1", tagGroup2.tag)
    }

    private fun entry(
        id: String = UUID.randomUUID().toString(),
        tag: String? = null,
    ) = JournalEntry(
        id = id,
        entryTime = Instant.parse("2024-11-30T15:00:00Z"),
        timeZone = TimeZone.UTC,
        text = "text",
        tag = tag,
        uploaded = false,
        autoTagged = false,
        deleted = false,
        reconciled = false,
    )
}
