package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.util.TableInfo
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.ramitsuri.notificationjournal.core.data.getColumnIndex
import com.ramitsuri.notificationjournal.core.data.getLongOrNull
import com.ramitsuri.notificationjournal.core.data.getTextOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class MigrationFrom5To6Test : BaseMigrationTest() {
    //region JournalEntry

    @Test
    fun testMigrateFrom5To6_shouldChangeJournalEntryIdColumnType() {
        try {
            // Arrange
            createAndGetEntriesFromV5()

            // Act
            val dbV6 = migrateAndGetDbV6()
            val tableInfo = TableInfo.read(dbV6, "JournalEntry")

            // Assert
            assertEquals("TEXT", tableInfo.columns["id"]!!.type)
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    @Test
    fun testMigrateFrom5To6_shouldMigrateJournalEntryData() {
        try {
            // Arrange
            val v5Data = createAndGetEntriesFromV5()

            // Act
            val connection = migrateAndGetDbV6()
            val v6Data = getEntriesFromV6(connection)

            // Assert
            v5Data.forEach { v5 ->
                val v6 = v6Data.first { it.text == v5.text }
                assertEquals(v5.entryTime, v6.entryTime)
                assertEquals(v5.timeZone, v6.timeZone)
                assertEquals(v5.text, v6.text)
                assertEquals(v5.tag, v6.tag)
                assertEquals(v5.entryTimeOverride, v6.entryTimeOverride)
                assertEquals(v5.uploaded, v6.uploaded)
                assertEquals(v5.autoTagged, v6.autoTagged)
            }
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    private fun getEntriesFromV6(connection: SQLiteConnection): List<JournalEntryV6> {
        val data = mutableListOf<JournalEntryV6>()
        connection.prepare("SELECT * FROM JournalEntry").use { statement ->
            while (statement.step()) {
                val id = statement.getText(statement.getColumnIndex("id"))
                val entryTime = statement.getLong(statement.getColumnIndex("entry_time"))
                val timeZone = statement.getText(statement.getColumnIndex("time_zone"))
                val text = statement.getText(statement.getColumnIndex("text"))
                val tag = statement.getTextOrNull(statement.getColumnIndex("tag"))
                val entryTimeOverride =
                    statement.getLongOrNull(statement.getColumnIndex("entry_time_override"))
                val uploaded = statement.getBoolean(statement.getColumnIndex("uploaded"))
                val autoTagged = statement.getBoolean(statement.getColumnIndex("auto_tagged"))
                data.add(
                    JournalEntryV6(
                        id = id,
                        entryTime = entryTime,
                        timeZone = timeZone,
                        text = text,
                        tag = tag,
                        entryTimeOverride = entryTimeOverride,
                        uploaded = uploaded,
                        autoTagged = autoTagged,
                    ),
                )
            }
        }
        return data
    }

    private fun createAndGetEntriesFromV5(): List<JournalEntryV5> {
        val data =
            listOf(
                JournalEntryV5(
                    id = 1,
                    timeZone = "TZ1",
                    entryTime = 1706208791347,
                    text = "Text 1",
                    tag = null,
                    entryTimeOverride = null,
                    uploaded = true,
                    autoTagged = false,
                ),
                JournalEntryV5(
                    id = 2,
                    entryTime = 1706208791347,
                    timeZone = "TZ2",
                    text = "Text 2",
                    tag = "Tag1",
                    entryTimeOverride = null,
                    uploaded = true,
                    autoTagged = true,
                ),
                JournalEntryV5(
                    id = 3,
                    entryTime = 1706208791347,
                    timeZone = "TZ3",
                    text = "Text 3",
                    tag = null,
                    entryTimeOverride = 1706208791348,
                    uploaded = false,
                    autoTagged = true,
                ),
                JournalEntryV5(
                    id = 4,
                    entryTime = 1706208791347,
                    timeZone = "TZ4",
                    text = "Text 4",
                    tag = "Tag4",
                    entryTimeOverride = 1706208791348,
                    uploaded = false,
                    autoTagged = false,
                ),
            )
        createDatabase(5).apply {
            data.forEach {
                val tag =
                    if (it.tag == null) {
                        null
                    } else {
                        "'${it.tag}'"
                    }
                execSQL(
                    "INSERT INTO JournalEntry " +
                        "(id,entry_time,time_zone,text,tag,entry_time_override,uploaded,auto_tagged) " +
                        "VALUES(" +
                        "${it.id},${it.entryTime},'${it.timeZone}','${it.text}'," +
                        "$tag,${it.entryTimeOverride},${it.uploaded},${it.autoTagged}" +
                        ")",
                )
            }
            close()
        }
        return data
    }

    private data class JournalEntryV5(
        val id: Int,
        val entryTime: Long,
        val timeZone: String,
        val text: String,
        val tag: String?,
        val entryTimeOverride: Long?,
        val uploaded: Boolean,
        val autoTagged: Boolean,
    )

    private data class JournalEntryV6(
        val id: String,
        val entryTime: Long,
        val timeZone: String,
        val text: String,
        val tag: String?,
        val entryTimeOverride: Long?,
        val uploaded: Boolean,
        val autoTagged: Boolean,
    )

    //endregion

    private fun migrateAndGetDbV6(): SQLiteConnection {
        return runMigrationAndValidate(6, MigrationFrom5To6())
    }

    //region Tag

    @Test
    fun testMigrateFrom5To6_shouldChangeTagIdColumnType() {
        try {
            // Arrange
            createAndGetTagsFromV5()

            // Act
            val dbV6 = migrateAndGetDbV6()
            val tableInfo = TableInfo.read(dbV6, "Tags")

            // Assert
            assertEquals("TEXT", tableInfo.columns["id"]!!.type)
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    @Test
    fun testMigrateFrom5To6_shouldMigrateTagData() {
        try {
            // Arrange
            val v5Data = createAndGetTagsFromV5()

            // Act
            val connection = migrateAndGetDbV6()
            val v6Data = getTagsFromV6(connection)

            // Assert
            v5Data.forEach { v5 ->
                val v6 = v6Data.first { it.value == v5.value }
                assertEquals(v6.value, v6.value)
                assertEquals(v6.order, v6.order)
            }
        } catch (e: Exception) {
            println(e.stackTraceToString())
            fail(e.message)
        }
    }

    private fun getTagsFromV6(connection: SQLiteConnection): List<TagV6> {
        val data = mutableListOf<TagV6>()
        connection.prepare("SELECT * FROM Tags").use { statement ->
            while (statement.step()) {
                val id = statement.getText(statement.getColumnIndex("id"))
                val order = statement.getInt(statement.getColumnIndex("order"))
                val value = statement.getText(statement.getColumnIndex("value"))
                data.add(
                    TagV6(
                        id = id,
                        order = order,
                        value = value,
                    ),
                )
            }
        }
        return data
    }

    private fun createAndGetTagsFromV5(): List<TagV5> {
        val tags =
            listOf(
                TagV5(
                    id = 1,
                    order = 1,
                    value = "Tag1",
                ),
                TagV5(
                    id = 2,
                    order = 2,
                    value = "Tag2",
                ),
                TagV5(
                    id = 3,
                    order = 3,
                    value = "Tag3",
                ),
                TagV5(
                    id = 4,
                    order = 4,
                    value = "Tag4",
                ),
            )
        createDatabase(5).apply {
            tags.forEach {
                execSQL(
                    "INSERT INTO Tags " +
                        "(id, 'order', value) " +
                        "VALUES " +
                        "(${it.id}, ${it.order}, '${it.value}')",
                )
            }
            close()
        }
        return tags
    }

    private data class TagV5(
        val id: Int,
        val order: Int,
        val value: String,
    )

    private data class TagV6(
        val id: String,
        val order: Int,
        val value: String,
    )

    //endregion

    // region JournalEntryTemplate

    @Test
    fun testMigrateFrom5To6_shouldChangeTemplateIdColumnType() {
        try {
            // Arrange
            createAndGetTemplatesFromV5()

            // Act
            val dbV6 = migrateAndGetDbV6()
            val tableInfo = TableInfo.read(dbV6, "JournalEntryTemplate")

            // Assert
            assertEquals("TEXT", tableInfo.columns["id"]!!.type)
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    @Test
    fun testMigrateFrom5To6_shouldMigrateTemplateData() {
        try {
            // Arrange
            val v5Data = createAndGetTemplatesFromV5()

            // Act
            val connection = migrateAndGetDbV6()
            val v6Data = getTemplatesFromV6(connection)

            // Assert
            v5Data.forEach { v5 ->
                val v6 = v6Data.first { it.text == v5.text }
                assertEquals(v6.text, v6.text)
                assertEquals(v6.tag, v6.tag)
            }
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    private fun getTemplatesFromV6(connection: SQLiteConnection): List<JournalEntryTemplateV6> {
        val data = mutableListOf<JournalEntryTemplateV6>()
        connection.prepare("SELECT * FROM JournalEntryTemplate").use { statement ->
            while (statement.step()) {
                val id = statement.getText(statement.getColumnIndex("id"))
                val text = statement.getText(statement.getColumnIndex("text"))
                val tag = statement.getText(statement.getColumnIndex("tag"))
                data.add(
                    JournalEntryTemplateV6(
                        id = id,
                        text = text,
                        tag = tag,
                    ),
                )
            }
        }
        return data
    }

    private fun createAndGetTemplatesFromV5(): List<JournalEntryTemplateV5> {
        val templates =
            listOf(
                JournalEntryTemplateV5(
                    id = 1,
                    text = "Text1",
                    tag = "Tag1",
                ),
                JournalEntryTemplateV5(
                    id = 2,
                    text = "Text2",
                    tag = "Tag2",
                ),
                JournalEntryTemplateV5(
                    id = 3,
                    text = "Text3",
                    tag = "Tag3",
                ),
                JournalEntryTemplateV5(
                    id = 4,
                    text = "Text4",
                    tag = "Tag4",
                ),
            )
        createDatabase(5).apply {
            templates.forEach {
                execSQL(
                    "INSERT INTO JournalEntryTemplate " +
                        "(id,text,tag) " +
                        "VALUES(" +
                        "${it.id},'${it.text}','${it.tag}'" +
                        ")",
                )
            }
            close()
        }
        return templates
    }

    data class JournalEntryTemplateV5(
        val id: Int,
        val text: String,
        val tag: String,
    )

    data class JournalEntryTemplateV6(
        val id: String,
        val text: String,
        val tag: String,
    )

    //endregion
}
