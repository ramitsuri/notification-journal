package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.util.TableInfo
import androidx.sqlite.SQLiteConnection
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MigrationFrom3To4Test : BaseMigrationTest() {
    @Test
    fun testMigrateFrom3To4() {
        try {
            // Arrange
            createDbVersionThree()

            // Act
            val dbV4 = migrateAndDbVersionFour()
            val tableInfo = TableInfo.read(dbV4, "JournalEntryTemplate")

            // Assert
            assertTrue(tableInfo.columns["id"]!!.isPrimaryKey)
            assertTrue(tableInfo.columns["id"]!!.notNull)
            assertTrue(tableInfo.columns["text"]!!.notNull)
            assertTrue(tableInfo.columns["tag"]!!.notNull)
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    private fun migrateAndDbVersionFour(): SQLiteConnection {
        return runMigrationAndValidate(4, MigrationFrom3To4())
    }

    private fun createDbVersionThree() {
        createDatabase(3)
    }
}
