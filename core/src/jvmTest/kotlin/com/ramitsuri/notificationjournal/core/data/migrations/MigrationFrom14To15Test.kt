package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.util.TableInfo
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MigrationFrom14To15Test : BaseMigrationTest() {
    @Test
    fun testVoiceNotesTableCreated() {
        try {
            // Arrange
            createDatabase(14)

            // Act
            val dbV15 = runMigrationAndValidate(15, MigrationFrom14To15())
            val tableInfoV15 = TableInfo.read(dbV15, "voice_notes")
            assertNotNull(tableInfoV15)
            assertTrue(tableInfoV15.columns.any { it.value.name == "id" })
            assertTrue(tableInfoV15.columns.any { it.value.name == "filePath" })
            assertTrue(tableInfoV15.columns.any { it.value.name == "recordedAt" })
            assertTrue(tableInfoV15.columns.any { it.value.name == "segmentIndex" })
            assertTrue(tableInfoV15.columns.any { it.value.name == "status" })
            assertTrue(tableInfoV15.columns.any { it.value.name == "errorMessage" })
        } catch (e: Exception) {
            fail(e.message)
        }
    }
}
