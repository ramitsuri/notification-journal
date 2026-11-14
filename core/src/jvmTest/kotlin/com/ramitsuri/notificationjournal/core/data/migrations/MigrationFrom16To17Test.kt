package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.util.TableInfo
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MigrationFrom16To17Test : BaseMigrationTest() {
    @Test
    fun testTagsTableIconColumnAdded() {
        try {
            // Arrange
            createDatabase(16)

            // Act
            val dbV17 = runMigrationAndValidate(17, MigrationFrom16To17())
            val tableInfoV17 = TableInfo.read(dbV17, "Tags")
            assertNotNull(tableInfoV17)
            assertTrue(tableInfoV17.columns.any { it.value.name == "icon" })
        } catch (e: Exception) {
            fail(e.message)
        }
    }
}
