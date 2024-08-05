package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.util.TableInfo
import androidx.sqlite.SQLiteConnection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MigrationFrom9To10Test : BaseMigrationTest() {
    @Test
    fun testMigration() {
        try {
            // Arrange
            createDbVersion9()

            // Act
            val dbV10 = migrateAndDbVersion10()
            val tableInfo = TableInfo.read(dbV10, "EntryConflict")

            // Assert
            assertTrue(tableInfo.columns["id"]!!.isPrimaryKey)
            assertTrue(tableInfo.columns["id"]!!.notNull)
            assertTrue(tableInfo.columns["entry_id"]!!.notNull)
            assertFalse(tableInfo.columns["tag"]!!.notNull)
            assertTrue(tableInfo.columns["text"]!!.notNull)
            assertTrue(tableInfo.columns["entry_time"]!!.notNull)
            assertTrue(tableInfo.columns["sender_name"]!!.notNull)
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    private fun migrateAndDbVersion10(): SQLiteConnection {
        return runMigrationAndValidate(10, MigrationFrom9To10())
    }

    private fun createDbVersion9() {
        createDatabase(9)
    }
}