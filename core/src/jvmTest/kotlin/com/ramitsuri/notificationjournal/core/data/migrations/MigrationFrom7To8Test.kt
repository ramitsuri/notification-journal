package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.util.TableInfo
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.use
import com.ramitsuri.notificationjournal.core.data.getColumnIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MigrationFrom7To8Test : BaseMigrationTest() {

    //region JournalEntry

    @Test
    fun testMigrateFrom7To8_shouldAddJournalEntryTemplateColumns() {
        try {
            // Arrange
            createAndGetTemplatesFromV7()

            // Act
            val dbV8 = migrateAndGetDbV8()
            val tableInfo = TableInfo.read(dbV8, "JournalEntryTemplate")

            // Assert
            assertTrue(tableInfo.columns.containsKey("display_text"))
            assertEquals("TEXT", tableInfo.columns["display_text"]!!.type)

            assertTrue(tableInfo.columns.containsKey("short_display_text"))
            assertEquals("TEXT", tableInfo.columns["short_display_text"]!!.type)
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    @Test
    fun testMigrateFrom7To8_shouldMigrateJournalEntryTemplateData() {
        try {
            // Arrange
            val v7Data = createAndGetTemplatesFromV7()

            // Act
            val connection = migrateAndGetDbV8()
            val v8Data = getTemplatesFromV8(connection)

            // Assert
            v7Data.forEach { v7 ->
                val v8 = v8Data.first { it.id == v7.id }
                assertEquals(v7.text, v8.text)
                assertEquals(v7.tag, v8.tag)
                assertTrue(v8.displayText.isEmpty())
                assertTrue(v8.shortDisplayText.isEmpty())
            }
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    private fun getTemplatesFromV8(connection: SQLiteConnection): List<TemplateV8> {
        val data = mutableListOf<TemplateV8>()
        connection.prepare("SELECT * FROM JournalEntryTemplate").use { statement ->
            while (statement.step()) {
                val id = statement.getText(statement.getColumnIndex("id"))
                val text = statement.getText(statement.getColumnIndex("text"))
                val tag = statement.getText(statement.getColumnIndex("tag"))
                val displayText = statement.getText(statement.getColumnIndex("display_text"))
                val shortDisplayText =
                    statement.getText(statement.getColumnIndex("short_display_text"))
                data.add(
                    TemplateV8(
                        id = id,
                        text = text,
                        tag = tag,
                        displayText = displayText,
                        shortDisplayText = shortDisplayText,
                    ),
                )
            }
        }
        return data
    }

    private fun createAndGetTemplatesFromV7(): List<TemplateV7> {
        val data = listOf(
            TemplateV7(
                id = "1",
                text = "Text 1",
                tag = "Tag1",
            ),
            TemplateV7(
                id = "2",
                text = "Text 2",
                tag = "Tag1",
            ),
            TemplateV7(
                id = "3",
                text = "Text 3",
                tag = "Tag3",
            ),
            TemplateV7(
                id = "4",
                text = "Text 4",
                tag = "Tag4",
            )
        )
        createDatabase(7).apply {
            data.forEach {
                execSQL(
                    "INSERT INTO JournalEntryTemplate " +
                            "(id,text,tag) " +
                            "VALUES(" +
                            "'${it.id}','${it.text}','${it.tag}'" +
                            ")"
                )
            }
            close()
        }
        return data
    }

    private data class TemplateV7(
        val id: String,
        val text: String,
        val tag: String,
    )

    private data class TemplateV8(
        val id: String,
        val text: String,
        val tag: String,
        val displayText: String,
        val shortDisplayText: String,
    )

    //endregion

    private fun migrateAndGetDbV8(): SQLiteConnection {
        return runMigrationAndValidate(8, MigrationFrom7To8())
    }
}