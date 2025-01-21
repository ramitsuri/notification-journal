package com.ramitsuri.notificationjournal.core.data.migrations

import androidx.room.util.TableInfo
import androidx.sqlite.SQLiteConnection
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MigrationFrom2To3Test : BaseMigrationTest() {
    @Test
    fun testMigrateFrom2To3() {
        try {
            // Arrange
            createDbVersionTwo()

            // Act
            val dbV3 = migrateAndDbVersionThree()
            val tableInfo = TableInfo.read(dbV3, "Tags")

            // Assert
            assertTrue(tableInfo.columns["id"]!!.isPrimaryKey)
            assertTrue(tableInfo.columns["id"]!!.notNull)
            assertTrue(tableInfo.columns["order"]!!.notNull)
            assertTrue(tableInfo.columns["value"]!!.notNull)
            val index = tableInfo.indices!!.first { it.name == "index_Tags_value" }
            assertNotNull(index)
            assertTrue(index.unique)
            assertTrue(index.columns.contains("value"))
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    private fun migrateAndDbVersionThree(): SQLiteConnection {
        return runMigrationAndValidate(3, MigrationFrom2To3())
    }

    private fun createDbVersionTwo() {
        createDatabase(2)
    }
}
