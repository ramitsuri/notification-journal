package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.util.TableInfo
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MigrationFrom13To14Test : BaseMigrationTest() {
    @Test
    fun testColumnRenamed() {
        try {
            // Arrange
            val dbV13 = createDatabase(13)
            val tableInfoV13 = TableInfo.read(dbV13, "JournalEntry")
            assertTrue(tableInfoV13.columns.any { it.value.name == "auto_tagged" })
            assertTrue(tableInfoV13.columns.none { it.value.name == "replaces_local" })

            // Act
            val dbV14 = runMigrationAndValidate(14, MigrationFrom13To14())
            val tableInfoV14 = TableInfo.read(dbV14, "JournalEntry")
            assertTrue(tableInfoV14.columns.none { it.value.name == "auto_tagged" })
            assertTrue(tableInfoV14.columns.any { it.value.name == "replaces_local" })
        } catch (e: Exception) {
            fail(e.message)
        }
    }
}
