package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.util.TableInfo
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MigrationFrom15To16Test : BaseMigrationTest() {
    @Test
    fun testLogsTableCreated() {
        try {
            // Arrange
            createDatabase(15)

            // Act
            val dbV16 = runMigrationAndValidate(16, MigrationFrom15To16())
            val tableInfoV16 = TableInfo.read(dbV16, "logs")
            assertNotNull(tableInfoV16)
            assertTrue(tableInfoV16.columns.any { it.value.name == "id" })
            assertTrue(tableInfoV16.columns.any { it.value.name == "time" })
            assertTrue(tableInfoV16.columns.any { it.value.name == "severity" })
            assertTrue(tableInfoV16.columns.any { it.value.name == "message" })
            assertTrue(tableInfoV16.columns.any { it.value.name == "tag" })
            assertTrue(tableInfoV16.columns.any { it.value.name == "errorMessage" })
            assertTrue(tableInfoV16.columns.any { it.value.name == "stackTrace" })
        } catch (e: Exception) {
            fail(e.message)
        }
    }
}
