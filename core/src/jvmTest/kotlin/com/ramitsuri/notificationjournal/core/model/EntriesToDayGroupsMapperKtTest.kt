package com.ramitsuri.notificationjournal.core.model

import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class EntriesToDayGroupsMapperKtTest {
    @Test
    fun testWithDifferentTags() {
        val tags =
            listOf(
                Tag(id = "id", value = "tag1", order = 1),
                Tag(id = "id", value = "tag2", order = 2),
            )
        val entries =
            listOf(
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
    fun testWithDifferentTagsShouldMatchSortOrder() {
        val tags =
            listOf(
                Tag(id = "id", value = "tag1", order = 1),
                Tag(id = "id", value = "tag2", order = 2),
            )
        val entries =
            listOf(
                entry(tag = tags[0].value),
                entry(tag = Tag.NO_TAG.value),
                entry(tag = "tag3"),
                entry(tag = tags[1].value),
            )

        val dayGroup = entries.toDayGroups(tags.map { it.value }).first()

        assertTrue(dayGroup.tagGroups[0].tag == Tag.NO_TAG.value)
        assertTrue(dayGroup.tagGroups[1].tag == "tag1")
        assertTrue(dayGroup.tagGroups[2].tag == "tag2")
        assertTrue(dayGroup.tagGroups[3].tag == "tag3")
    }

    @Test
    fun testWithNoTagVariationTags() {
        val entries =
            listOf(
                entry(tag = Tag.NO_TAG.value),
                entry(tag = Tag.NO_TAG.value),
            )

        val dayGroup = entries.toDayGroups().first()

        val tagGroup = dayGroup.tagGroups.first()
        assertEquals(2, tagGroup.entries.size)
    }

    @Test
    fun testWithNoTagsShouldHaveSingleNoTagGroup() {
        val entries =
            listOf(
                entry(tag = Tag.NO_TAG.value),
                entry(tag = Tag.NO_TAG.value),
            )

        val dayGroup = entries.toDayGroups().first()

        assertEquals(1, dayGroup.tagGroups.size)
    }

    @Test
    fun testWithNoTagAndTag() {
        val entries =
            listOf(
                entry(tag = "tag1"),
                entry(tag = Tag.NO_TAG.value),
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
        val entries =
            listOf(
                entry(tag = "tag1"),
                entry(tag = Tag.NO_TAG.value),
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
        tag: String = Tag.NO_TAG.value,
    ) = JournalEntry(
        id = id,
        entryTime = LocalDateTime.parse("2024-11-30T15:00:00"),
        text = "text",
        tag = tag,
        uploaded = false,
        autoTagged = false,
        deleted = false,
        reconciled = false,
    )
}
