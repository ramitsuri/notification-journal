package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.util.TableInfo
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MigrationFrom17To18Test : BaseMigrationTest() {
    @Test
    fun testReconcileChecksTableCreated() {
        try {
            // Arrange
            createDatabase(17)

            // Act
            val dbV18 = runMigrationAndValidate(18, MigrationFrom17To18())
            val tableInfoV18 = TableInfo.read(dbV18, "reconcile_checks")
            assertNotNull(tableInfoV18)
            assertTrue(tableInfoV18.columns.any { it.value.name == "id" })
            assertTrue(tableInfoV18.columns.any { it.value.name == "tag" })
            assertTrue(tableInfoV18.columns.any { it.value.name == "days_of_week" })
            assertTrue(tableInfoV18.columns.any { it.value.name == "match_type" })
            assertTrue(tableInfoV18.columns.any { it.value.name == "query" })
            assertTrue(tableInfoV18.columns.any { it.value.name == "enabled" })
        } catch (e: Exception) {
            fail(e.message)
        }
    }
}
