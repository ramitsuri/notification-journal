package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.ramitsuri.notificationjournal.core.data.getColumnIndex
import com.ramitsuri.notificationjournal.core.data.getLongOrNull
import com.ramitsuri.notificationjournal.core.data.getTextOrNull
import java.util.UUID

// Changes auto generated int primary key to UUID string primary key for JournalEntry, Tags,
// Templates tables
class MigrationFrom5To6 : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        val entries = getExistingJournalEntries(connection)
        connection.execSQL("DROP TABLE `JournalEntry`")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `JournalEntry` " +
                "(" +
                "`id` TEXT NOT NULL, " +
                "`entry_time` INTEGER NOT NULL, " +
                "`time_zone` TEXT NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "`tag` TEXT, " +
                "`entry_time_override` INTEGER, " +
                "`uploaded` INTEGER NOT NULL DEFAULT 0, " +
                "`auto_tagged` INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(`id`)" +
                ")",
        )
        entries.forEach { entry ->
            connection.prepare(
                "INSERT INTO JournalEntry " +
                    "(id, entry_time, time_zone, text, tag, entry_time_override, uploaded, auto_tagged) " +
                    "VALUES " +
                    " (?, ?, ?, ?, ?, ?, ?, ?)",
            ).use { statement ->
                statement.bindText(index = 1, value = UUID.randomUUID().toString())
                statement.bindLong(index = 2, value = entry.entryTimeMillis)
                statement.bindText(index = 3, value = entry.timeZone)
                statement.bindText(index = 4, value = entry.text)
                if (entry.tag == null) {
                    statement.bindNull(index = 5)
                } else {
                    statement.bindText(index = 5, value = entry.tag)
                }
                if (entry.entryTimeOverride == null) {
                    statement.bindNull(index = 6)
                } else {
                    statement.bindLong(index = 6, entry.entryTimeOverride)
                }
                statement.bindBoolean(index = 7, value = entry.uploaded)
                statement.bindBoolean(index = 8, value = entry.autoTagged)
                statement.step()
            }
        }

        val tags = getExistingTags(connection)
        connection.execSQL("DROP TABLE `Tags`")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `Tags` " +
                "(" +
                "`id` TEXT NOT NULL, " +
                "`order` INTEGER NOT NULL, " +
                "`value` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`)" +
                ")",
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_Tags_value` ON `Tags` (`value`)",
        )
        tags.forEach { tag ->
            connection.prepare(
                "INSERT INTO Tags " +
                    "(id, 'order', value) " +
                    "VALUES " +
                    " (?, ?, ?)",
            ).use { statement ->
                statement.bindText(index = 1, value = UUID.randomUUID().toString())
                statement.bindInt(index = 2, value = tag.order)
                statement.bindText(index = 3, value = tag.value)
                statement.step()
            }
        }

        val templates = getExistingTemplates(connection)
        connection.execSQL("DROP TABLE `JournalEntryTemplate`")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `JournalEntryTemplate` " +
                "(" +
                "`id` TEXT NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "`tag` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`)" +
                ")",
        )
        templates.forEach { template ->
            connection.prepare(
                "INSERT INTO JournalEntryTemplate " +
                    "(id, text, tag) " +
                    "VALUES " +
                    " (?, ?, ?)",
            ).use { statement ->
                statement.bindText(index = 1, value = UUID.randomUUID().toString())
                statement.bindText(index = 2, value = template.text)
                statement.bindText(index = 3, value = template.tag)
                statement.step()
            }
        }
    }

    private fun getExistingJournalEntries(connection: SQLiteConnection): List<JournalEntryV5> {
        val entries = mutableListOf<JournalEntryV5>()
        connection.prepare("SELECT * FROM JournalEntry").use { statement ->
            while (statement.step()) {
                try {
                    val idColumn = statement.getColumnIndex("id")
                    val id = statement.getInt(idColumn)

                    val entryTimeColumn = statement.getColumnIndex("entry_time")
                    val entryTimeMillis = statement.getLong(entryTimeColumn)

                    val zoneIdColumn = statement.getColumnIndex("time_zone")
                    val zoneId = statement.getText(zoneIdColumn)

                    val textColumn = statement.getColumnIndex("text")
                    val text = statement.getText(textColumn)

                    val tagColumn = statement.getColumnIndex("tag")
                    val tag = statement.getTextOrNull(tagColumn)

                    val entryTimeOverrideColumn = statement.getColumnIndex("entry_time_override")
                    val entryTimeOverride = statement.getLongOrNull(entryTimeOverrideColumn)

                    val uploadedColumn = statement.getColumnIndex("uploaded")
                    val uploaded = statement.getBoolean(uploadedColumn)

                    val autoTaggedColumn = statement.getColumnIndex("auto_tagged")
                    val autoTagged = statement.getBoolean(autoTaggedColumn)

                    entries.add(
                        JournalEntryV5(
                            id = id,
                            entryTimeMillis = entryTimeMillis,
                            timeZone = zoneId,
                            text = text,
                            tag = tag,
                            entryTimeOverride = entryTimeOverride,
                            uploaded = uploaded,
                            autoTagged = autoTagged,
                        ),
                    )
                } catch (e: Exception) {
                    // Do nothing. Continue reading the ones we can
                }
            }
        }
        return entries
    }

    private fun getExistingTags(connection: SQLiteConnection): List<TagV5> {
        val tags = mutableListOf<TagV5>()
        connection.prepare("SELECT * FROM Tags").use { statement ->
            while (statement.step()) {
                try {
                    val idColumn = statement.getColumnIndex("id")
                    val id = statement.getInt(idColumn)

                    val orderColumn = statement.getColumnIndex("order")
                    val order = statement.getInt(orderColumn)

                    val valueColumn = statement.getColumnIndex("value")
                    val value = statement.getText(valueColumn)

                    tags.add(TagV5(id, order, value))
                } catch (e: Exception) {
                    // Do nothing. Continue reading the ones we can
                }
            }
        }
        return tags
    }

    private fun getExistingTemplates(connection: SQLiteConnection): List<TemplateV5> {
        val templates = mutableListOf<TemplateV5>()
        connection.prepare("SELECT * FROM JournalEntryTemplate").use { statement ->
            while (statement.step()) {
                try {
                    val idColumn = statement.getColumnIndex("id")
                    val id = statement.getInt(idColumn)

                    val textColumn = statement.getColumnIndex("text")
                    val text = statement.getText(textColumn)

                    val tagColumn = statement.getColumnIndex("tag")
                    val tag = statement.getText(tagColumn)

                    templates.add(TemplateV5(id, text, tag))
                } catch (e: Exception) {
                    // Do nothing. Continue reading the ones we can
                }
            }
        }
        return templates
    }

    private data class JournalEntryV5(
        val id: Int,
        val entryTimeMillis: Long,
        val timeZone: String,
        val text: String,
        val tag: String?,
        val entryTimeOverride: Long?,
        val uploaded: Boolean,
        val autoTagged: Boolean,
    )

    private data class TagV5(
        val id: Int,
        val order: Int,
        val value: String,
    )

    private data class TemplateV5(
        val id: Int,
        val text: String,
        val tag: String,
    )
}
